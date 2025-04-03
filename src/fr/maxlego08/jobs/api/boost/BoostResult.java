package fr.maxlego08.jobs.api.boost;

public record BoostResult(double experience, double money, Boost boost) {

    /**
     * Checks if the boost is not null.
     * This function will return true if the boost is not null, else false.
     *
     * @return true, if the boost is not null, else false
     */
    public boolean hasBoost() {
        return this.boost != null;
    }

}
