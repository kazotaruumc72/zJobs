package fr.maxlego08.jobs.api.utils;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public record ValueInformation(String material, String name, double experience, double money, Consumer<ItemStack> applyItemStack) {
}
