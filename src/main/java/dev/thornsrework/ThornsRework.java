package dev.thornsrework;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.ServerTask;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThornsRework implements ModInitializer {
    public static final String ID = "thornsrework";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final Identifier PACK = Identifier.of(ID, "thorns");
    public static final Identifier RETALIATE = Identifier.of(ID, "retaliate");

    @Override
    public void onInitialize() {
        Registry.register(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, RETALIATE, RetaliationEffect.CODEC);

        var container = FabricLoader.getInstance().getModContainer(ID).orElseThrow();
        if (!ResourceManagerHelper.registerBuiltinResourcePack(PACK, container,
                Text.literal("Thorns Rework"), ResourcePackActivationType.ALWAYS_ENABLED)) {
            throw new IllegalStateException("Thorns Rework could not register its built-in data pack");
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> DebugMode.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> DebugMode.start(server.isDedicated()));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ThornsConflict.verify(server.getResourceManager()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (!success) {
                return;
            }
            try {
                ThornsConflict.verify(resources);
            } catch (ThornsConflict.Conflict conflict) {
                // A completion callback is inside a future. Queue the fatal error on the server loop.
                server.send(new ServerTask(server.getTicks(), () -> { throw conflict; }));
                throw conflict;
            }
        });
    }
}
