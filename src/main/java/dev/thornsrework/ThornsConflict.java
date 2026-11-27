package dev.thornsrework;

import java.util.List;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

final class ThornsConflict {
    private static final Identifier FILE = Identifier.ofVanilla("enchantment/thorns.json");

    private ThornsConflict() {
    }

    static void verify(ResourceManager resources) {
        String expected = ThornsRework.PACK.toString();
        List<String> providers = resources.getAllResources(FILE).stream().map(Resource::getPackId).toList();
        List<String> conflicts = providers.stream().filter(id -> !id.equals("vanilla") && !id.equals(expected)).toList();
        String winner = resources.getResource(FILE).map(Resource::getPackId).orElse("<missing>");
        if (!conflicts.isEmpty() || !winner.equals(expected)) {
            throw new Conflict("Thorns Rework conflict for minecraft:thorns. Conflicting packs/mods: " + conflicts
                    + ". Providers: " + providers + ". Active provider: " + winner
                    + ". Remove the conflicting data pack or mod, keep " + expected + " enabled, then restart the server.");
        }
    }

    static final class Conflict extends Error {
        Conflict(String message) {
            super(message);
        }
    }
}
