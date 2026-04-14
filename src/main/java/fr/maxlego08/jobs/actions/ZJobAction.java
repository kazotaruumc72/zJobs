package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.utils.ValueInformation;
import fr.maxlego08.jobs.placeholder.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ZJobAction<T> implements JobAction<T> {

    protected final T target;
    private final double experience;
    private final double money;
    private final String displayMaterial;
    private String displayName;
    private String experienceFormula;
    private String moneyFormula;

    public ZJobAction(T target, double experience, double money, String displayMaterial) {
        this.target = target;
        this.experience = experience;
        this.money = money;
        this.displayMaterial = displayMaterial;
    }

    @Override
    public T getTarget() {
        return this.target;
    }

    @Override
    public double getExperience() {
        return this.experience;
    }

    @Override
    public double getMoney() {
        return this.money;
    }

    @Override
    public double getExperience(Player player) {
        return resolveFormula(this.experienceFormula, this.experience, player);
    }

    @Override
    public double getMoney(Player player) {
        return resolveFormula(this.moneyFormula, this.money, player);
    }

    private double resolveFormula(String formula, double fallback, Player player) {
        if (formula == null || player == null) {
            return fallback;
        }
        String resolved = Placeholder.getPlaceholder().setPlaceholders(player, formula);
        try {
            return Double.parseDouble(resolved);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public void setExperienceFormula(String experienceFormula) {
        this.experienceFormula = experienceFormula;
    }

    public void setMoneyFormula(String moneyFormula) {
        this.moneyFormula = moneyFormula;
    }

    @Override
    public String toString() {
        return "ZJobAction{" +
                "target=" + target +
                ", experience=" + experience +
                ", money=" + money +
                '}';
    }

    @Override
    public void applyItemStack(ItemStack itemStack) {

    }

    @Override
    public String getDisplayMaterial() {
        return this.displayMaterial == null ? "STONE" : this.displayMaterial;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public ValueInformation toValueInformation() {
        return new ValueInformation(this.getDisplayMaterial(), this.getDisplayName(), this.getExperience(), this.getMoney(), this::applyItemStack);
    }
}
