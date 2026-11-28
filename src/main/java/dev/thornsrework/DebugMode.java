package dev.thornsrework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
                                .executes(ctx -> update(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))));
    }

    private static int status(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Thorns Rework debug=" + enabled() + " (configured=" + configured
                + ", dedicatedOverride=" + forced + "), durabilityCostPerLevel=" + RetaliationEffect.DURABILITY_COST_PER_LEVEL
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
                configured = enabled;
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
        }
        var json = GSON.fromJson(Files.readString(FILE), com.google.gson.JsonObject.class);
        if (json == null || !json.has("debug") || !json.get("debug").isJsonPrimitive()
                || !json.getAsJsonPrimitive("debug").isBoolean()) {
            throw new IllegalArgumentException("Expected a boolean 'debug' field");
        }
        configured = json.get("debug").getAsBoolean();
    }

    private static void save(boolean enabled) throws IOException {
        Files.createDirectories(FILE.getParent());
        Path temporary = Files.createTempFile(FILE.getParent(), "thornsrework-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(new Config(enabled)) + System.lineSeparator());
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

    private record Config(boolean debug) {
    }
}

