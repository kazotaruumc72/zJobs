package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.permissibles.HasLevelPermissible;
import fr.maxlego08.jobs.zmenu.permissibles.HasPrestigePermissible;
import fr.maxlego08.menu.api.loader.PermissibleLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.requirement.Permissible;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;
import java.util.List;

public class HasLevelLoader extends PermissibleLoader {

    private final JobsPlugin plugin;

    public HasLevelLoader(JobsPlugin plugin) {
        super("zjobs has level");
        this.plugin = plugin;
    }

    @Override
    public Permissible load(String path, TypedMapAccessor accessor, File file) {
        String jobName = accessor.getString("job");
        String level = accessor.getString("level");
        List<Action> denyActions = loadAction(this.plugin.getButtonManager(), accessor, "deny", path, file);
        List<Action> successActions = loadAction(this.plugin.getButtonManager(), accessor, "success", path, file);
        return new HasLevelPermissible(denyActions, successActions, this.plugin, jobName, level);
    }
}
