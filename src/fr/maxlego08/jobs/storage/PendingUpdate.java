package fr.maxlego08.jobs.storage;

import fr.maxlego08.jobs.api.boost.Boost;
import fr.maxlego08.jobs.api.players.PlayerJob;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PendingUpdate {

    private final Map<String, PlayerJob> jobs = new ConcurrentHashMap<>();
    private final Set<Boost> boosts = new LinkedHashSet<>();

    public Map<String, PlayerJob> getJobs() {
        return jobs;
    }

    public Set<Boost> getBoosts() {
        return boosts;
    }

    public void remove(String jobId) {
        this.jobs.remove(jobId);
    }

    public void setJob(String jobId, PlayerJob playerJob) {
        this.jobs.put(jobId, playerJob);
    }
}
