package fr.maxlego08.jobs.api.enums;

import fr.maxlego08.jobs.api.actions.ActionInfo;
import fr.maxlego08.jobs.api.actions.BrewAction;
import fr.maxlego08.jobs.api.actions.CommandAction;
import fr.maxlego08.jobs.api.actions.CustomAction;
import fr.maxlego08.jobs.api.actions.EnchantAction;
import fr.maxlego08.jobs.api.actions.EntityAction;
import fr.maxlego08.jobs.api.actions.MaterialAction;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.BrewEvent;

public enum JobActionType {

    BLOCK_BREAK,
    BLOCK_PLACE,
    KILL_ENTITY,
    FARMING,
    FISHING,
    COMMAND,
    TAME,
    ENCHANT,
    BREW,
    SMELT,
    STRIPLOGS,
    ANVIL_REPAIR,
    SMITHING,
    CUSTOM,
    RAFINE,
    FORGE
    ;

    public ActionInfo<?> toAction(Object target) {
        return switch (this) {
            case BLOCK_BREAK, BLOCK_PLACE, FARMING, FISHING, SMELT, STRIPLOGS, ANVIL_REPAIR, SMITHING -> {
                if (target instanceof String s) yield new CustomAction(this, s);
                yield new MaterialAction(this, (Material) target);
            }
            case KILL_ENTITY, TAME -> {
                if (target instanceof String s) yield new CustomAction(this, s);
                if (target instanceof EntityType et) yield new EntityAction(this, et);
                if (target instanceof Entity e) yield new EntityAction(this, e.getType());
                yield new EntityAction(this, EntityType.UNKNOWN);
            }
            case COMMAND -> new CommandAction(target == null ? "" : (String) target);
            case ENCHANT -> new EnchantAction(this, (EnchantItemEvent) target);
            case BREW -> new BrewAction(this, (BrewEvent) target);
            case CUSTOM, RAFINE, FORGE -> new CustomAction(this, target == null ? "" : target.toString());
        };
    }

    public boolean isMaterial() {
        return switch (this) {
            case BLOCK_BREAK, BLOCK_PLACE, FARMING, FISHING, SMELT, STRIPLOGS, ANVIL_REPAIR, SMITHING -> true;
            case COMMAND, KILL_ENTITY, TAME, ENCHANT, BREW, CUSTOM, RAFINE, FORGE -> false;
        };
    }

    public boolean isEntityType() {
        return this == KILL_ENTITY || this == TAME;
    }
}
