package fr.maxlego08.jobs.placeholder;

import org.bukkit.entity.Player;

/**
 * Represents a placeholder that automatically resolves values based on a prefix string.
 */
public class AutoPlaceholder {

    private final String startWith;
    private final ReturnBiConsumer<Player, String, String> biConsumer;
    private final ReturnConsumer<Player, String> consumer;

    /**
     * Constructs an {@code AutoPlaceholder} with a bi-consumer resolver.
     *
     * @param startWith  the prefix this placeholder matches.
     * @param biConsumer the bi-consumer used to resolve the placeholder value.
     */
    public AutoPlaceholder(String startWith, ReturnBiConsumer<Player, String, String> biConsumer) {
        super();
        this.startWith = startWith;
        this.biConsumer = biConsumer;
        this.consumer = null;
    }

    /**
     * Constructs an {@code AutoPlaceholder} with a consumer resolver.
     *
     * @param startWith the prefix this placeholder matches.
     * @param consumer  the consumer used to resolve the placeholder value.
     */
    public AutoPlaceholder(String startWith, ReturnConsumer<Player, String> consumer) {
        this.startWith = startWith;
        this.biConsumer = null;
        this.consumer = consumer;
    }

    /**
     * Gets the prefix string this placeholder matches against.
     *
     * @return the startWith prefix.
     */
    public String getStartWith() {
        return startWith;
    }

    /**
     * Gets the bi-consumer resolver for this placeholder.
     *
     * @return the biConsumer, or {@code null} if a simple consumer is used.
     */
    public ReturnBiConsumer<Player, String, String> getBiConsumer() {
        return biConsumer;
    }

    public ReturnConsumer<Player, String> getConsumer() {
        return this.consumer;
    }

    public String accept(Player player, String value) {
        if (this.consumer != null) return this.consumer.accept(player);
        if (this.biConsumer != null) return this.biConsumer.accept(player, value);
        return "Error with consumer !";
    }

    public boolean startsWith(String string) {
        return this.consumer != null ? this.startWith.equalsIgnoreCase(string) : string.startsWith(this.startWith);
    }
}
