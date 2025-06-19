package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.api.Job;
import fr.maxlego08.jobs.api.JobManager;
import fr.maxlego08.jobs.zmenu.buttons.JobInfoButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class JobInfoLoader extends ButtonLoader {
    private final JobsPlugin plugin;

    public JobInfoLoader(JobsPlugin plugin) {
        super(plugin, "ZJOBS_INFO");
        this.plugin = plugin;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        String jobId = configuration.getString(path + "job");
        JobManager jobManager = plugin.getJobManager();
        var optional = jobManager.getJob(jobId);
        if (optional.isEmpty()) {
            plugin.getLogger().severe("Impossible to find the job id " + jobId + " in " + path);
        }
        Job job = optional.orElse(null);
        return new JobInfoButton(plugin, job);
    }
}
