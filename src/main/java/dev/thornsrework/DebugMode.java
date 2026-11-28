package dev.thornsrework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class DebugMode {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("thornsrework.json");
    private static final String PROPERTY = "thornsrework.debug";
    private static boolean configured;
    private static boolean forced;
    private static double beyondSpawnPercent = 5.0;
    private static double overlordSpawnPercent = 0.2;

    private DebugMode() {
    }

    static void start(boolean dedicated) {
        configured = false;
        forced = dedicated && Boolean.getBoolean(PROPERTY);
        try {
            reload();
        } catch (IOException | RuntimeException e) {
            ThornsRework.LOGGER.warn("Could not load {}. Check /thornsrework debug status.", FILE, e);
        }
    }

    public static boolean enabled() {
        return forced || configured;
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("thornsrework").requires(source -> source.hasPermissionLevel(2))
                .then(literal("debug")
                        .executes(ctx -> status(ctx.getSource()))
                        .then(literal("status").executes(ctx -> status(ctx.getSource())))
                        .then(literal("reload").executes(ctx -> update(ctx.getSource(), null)))
                        .then(literal("set").then(argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> update(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(literal("ThornMonsterSpawnRate")
                                .executes(ctx -> enabled() ? rateStatus(ctx.getSource()) : requireDebug(ctx.getSource()))
                                .then(literal("Beyond").requires(source -> debugRequired(source)).then(literal("change")
                                        .then(argument("num", DoubleArgumentType.doubleArg(0, 100))
                                                .executes(ctx -> setSpawnRate(ctx.getSource(), "Beyond",
                                                        DoubleArgumentType.getDouble(ctx, "num"))))))
                                .then(literal("Overlord").requires(source -> debugRequired(source)).then(literal("change")
                                        .then(argument("num", DoubleArgumentType.doubleArg(0, 100))
                                                .executes(ctx -> setSpawnRate(ctx.getSource(), "Overlord",
                                                        DoubleArgumentType.getDouble(ctx, "num")))))))));
    }

    private static boolean debugRequired(ServerCommandSource source) {
        if (enabled()) {
            return true;
        }
        source.sendError(Text.literal("需要使用/thornsrework debug set true开启调试模式 en-us:Use /thornsrework debug set true to enable debug mode."));
        return false;
    }

    private static int requireDebug(ServerCommandSource source) {
        source.sendError(Text.literal("需要使用/thornsrework debug set true开启调试模式 en-us:Use /thornsrework debug set true to enable debug mode."));
        return 0;
    }

    private static int setSpawnRate(ServerCommandSource source, String tier, double percent) {
        if (!enabled()) {
            source.sendError(Text.literal("需要使用/thornsrework debug set true开启调试模式 en-us:Use /thornsrework debug set true to enable debug mode."));
            return 0;
        }
        if (!Double.isFinite(percent) || percent < 0 || percent > 100) {
            source.sendError(Text.literal("Spawn percentage must be finite and between 0 and 100."));
            return 0;
        }
        double oldBeyond = beyondSpawnPercent;
        double oldOverlord = overlordSpawnPercent;
        if (tier.equals("Beyond")) {
            beyondSpawnPercent = percent;
        } else {
            overlordSpawnPercent = percent;
        }
        try {
            saveSpawnRates();
        } catch (IOException e) {
            beyondSpawnPercent = oldBeyond;
            overlordSpawnPercent = oldOverlord;
            source.sendError(Text.literal("Could not save spawn rates: " + e.getMessage()));
            return 0;
        }
        source.sendFeedback(() -> Text.literal("Thorns Rework " + tier + " natural spawn rate: "
                + percent + "% (Hard difficulty only)."), false);
        ThornsRework.LOGGER.info("Set Thorns Rework {} natural spawn chance to {}%", tier, percent);
        return 1;
    }

    static double beyondSpawnPercent() {
        return beyondSpawnPercent;
    }

    static double overlordSpawnPercent() {
        return overlordSpawnPercent;
    }

    private static int status(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Thorns Rework debug=" + enabled() + " (configured=" + configured
                + ", dedicatedOverride=" + forced + "), durabilityCostPerLevel=" + RetaliationEffect.DURABILITY_COST_PER_LEVEL
                + ", Beyond=" + beyondSpawnPercent + "%, Overlord=" + overlordSpawnPercent + "%"
                + ", bareHandDamage=" + RetaliationEffect.MIN_DAMAGE + ".." + RetaliationEffect.MAX_DAMAGE
                + ", projectileEPF=8.333333, config=" + FILE), false);
        return 1;
    }

    private static int update(ServerCommandSource source, Boolean enabled) {
        try {
            if (enabled == null) {
                reload();
            } else {
                save(enabled);
            }
            return status(source);
        } catch (IOException | RuntimeException e) {
            source.sendError(Text.literal("Could not update " + FILE + ": " + e.getMessage()));
            return 0;
        }
    }

    private static void reload() throws IOException {
        if (Files.notExists(FILE)) {
            save(false);
            return;
        }
        var json = GSON.fromJson(Files.readString(FILE), com.google.gson.JsonObject.class);
        if (json == null || !json.has("debug") || !json.get("debug").isJsonPrimitive()
                || !json.getAsJsonPrimitive("debug").isBoolean()) {
            throw new IllegalArgumentException("Expected a boolean 'debug' field");
        }
        configured = json.get("debug").getAsBoolean();
        beyondSpawnPercent = readPercentage(json, "beyondSpawnPercent", 5.0);
        overlordSpawnPercent = readPercentage(json, "overlordSpawnPercent", 0.2);
    }

    private static void save(boolean enabled) throws IOException {
        configured = enabled;
        saveConfig();
    }

    private static int rateStatus(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("ThornsRework spawn rate: Beyond=" + beyondSpawnPercent
                + "%, Overlord=" + overlordSpawnPercent + "% (Hard, natural zombies and skeletons only)."), false);
        return 1;
    }

    private static final String CONFIGURE_MONSTER_SPAWN_RATE = "ThornMonsterSpawnRate";

    static String configurationKey() {
        return CONFIGURE_MONSTER_SPAWN_RATE;
    }

    private static double readPercentage(com.google.gson.JsonObject json, String field, double defaultValue) {
        if (!json.has(field)) {
            return defaultValue;
        }
        if (!json.get(field).isJsonPrimitive() || !json.getAsJsonPrimitive(field).isNumber()) {
            throw new IllegalArgumentException(field + " must be a number between 0 and 100");
        }
        double value = json.get(field).getAsDouble();
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException(field + " must be a number between 0 and 100");
        }
        return value;
    }

    private static void saveSpawnRates() throws IOException {
        saveConfig();
    }

    private static void saveConfig() throws IOException {
        Files.createDirectories(FILE.getParent());
        Path temporary = Files.createTempFile(FILE.getParent(), "thornsrework-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(new Config(configured, beyondSpawnPercent, overlordSpawnPercent))
                    + System.lineSeparator());
            Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void trace(ServerWorld world, int level, Entity attacker, String branch, Number before,
                             Number after, Number expected, Number actual) {
        if (enabled()) {
            String hand = attacker instanceof LivingEntity living
                    ? Registries.ITEM.getId(living.getMainHandStack().getItem()).toString() : "<none>";
            trace(world, level, hand, branch, before, after, expected, actual);
        }
    }

    static void trace(ServerWorld world, int level, String hand, String branch, Number before, Number after,
                      Number expected, Number actual) {
        if (enabled()) {
            String line = "[Thorns Rework] branch=" + branch + " level=" + level + " mainhand=" + hand
                    + " before=" + before + " after=" + after + " expected=" + expected + " actual=" + actual;
            ThornsRework.LOGGER.info(line);
            Text message = Text.literal(line);
            world.getServer().getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(message, false));
        }
    }

    private record Config(boolean debug, double beyondSpawnPercent, double overlordSpawnPercent) {
    }
}

