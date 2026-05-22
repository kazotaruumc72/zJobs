package fr.maxlego08.jobs.hooks;

import io.github.pigaut.orestack.api.Orestack;
import io.github.pigaut.orestack.api.OrestackAPI;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class OrestackHook {

    public boolean isGenerator(Block block) {
        return block != null && isGenerator(block.getLocation());
    }

    public boolean isGenerator(Location location) {
        try {
            OrestackAPI api = Orestack.getAPI();
            return api != null && api.isGenerator(location);
        } catch (IllegalStateException notReadyYet) {
            return false;
        }
    }
}