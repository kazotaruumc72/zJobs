package fr.maxlego08.jobs.hooks;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import org.bukkit.block.Block;

public class NexoHook {

    public boolean isNexoBlock(Block block) {
        return NexoBlocks.isCustomBlock(block);
    }

    public String getNexoBlockId(Block block) {
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block.getLocation());
        return mechanic != null ? mechanic.getItemID() : null;
    }
}
