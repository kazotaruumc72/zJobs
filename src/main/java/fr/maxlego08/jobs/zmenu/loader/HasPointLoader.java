package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.permissibles.HasPointPermissible;
import fr.maxlego08.menu.api.loader.PermissibleLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.requirement.Permissible;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;
import java.util.List;

public class HasPointLoader extends PermissibleLoader {

    private final JobsPlugin plugin;

    public HasPointLoader(JobsPlugin plugin) {
        super("zjobs has points");
        this.plugin = plugin;
    }

    @Override
    public Permissible load(String path, TypedMapAccessor accessor, File file) {
        String points = accessor.getString("points");
        List<Action> denyActions = loadAction(this.plugin.getButtonManager(), accessor, "deny", path, file);
        List<Action> successActions = loadAction(this.plugin.getButtonManager(), accessor, "success", path, file);
        return new HasPointPermissible(plugin, points, denyActions, successActions);
    }
}
