package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.permissibles.HasPrestigePermissible;
import fr.maxlego08.menu.api.loader.PermissibleLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.requirement.Permissible;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;
import java.util.List;

public class HasPrestigeLoader extends PermissibleLoader {

    private final JobsPlugin plugin;

    public HasPrestigeLoader(JobsPlugin plugin) {
        super("zjobs has prestige");
        this.plugin = plugin;
    }

    @Override
    public Permissible load(String path, TypedMapAccessor accessor, File file) {
        String jobName = accessor.getString("job");
        String prestige = accessor.getString("prestige");
        List<Action> denyActions = loadAction(this.plugin.getButtonManager(), accessor, "deny", path, file);
        List<Action> successActions = loadAction(this.plugin.getButtonManager(), accessor, "success", path, file);
        return new HasPrestigePermissible(denyActions, successActions, this.plugin, jobName, prestige);
    }
}
