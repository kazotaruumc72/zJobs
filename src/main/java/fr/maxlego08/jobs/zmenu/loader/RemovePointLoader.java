package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.actions.AddPointsAction;
import fr.maxlego08.jobs.zmenu.actions.RemovePointsAction;
import fr.maxlego08.menu.api.loader.ActionLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;

public class RemovePointLoader extends ActionLoader {

    private final JobsPlugin plugin;

    public RemovePointLoader(JobsPlugin plugin) {
        super("zjobs_remove_points", "zjobs remove points");
        this.plugin = plugin;
    }

    @Override
    public Action load(String path, TypedMapAccessor accessor, File file) {
        String points = accessor.getString("points");
        return new RemovePointsAction(plugin, points);
    }
}
