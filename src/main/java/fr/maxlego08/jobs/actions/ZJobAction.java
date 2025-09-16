package fr.maxlego08.jobs.actions;

import fr.maxlego08.jobs.api.JobAction;
import fr.maxlego08.jobs.api.utils.ValueInformation;
import org.bukkit.inventory.ItemStack;

public abstract class ZJobAction<T> implements JobAction<T> {

    protected final T target;
    private final double experience;
    private final double money;
    private final String displayMaterial;
    private String displayName;

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
