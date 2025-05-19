package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.actions.AddPointsAction;
import fr.maxlego08.menu.api.loader.ActionLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;

public class AddPointLoader extends ActionLoader {

    private final JobsPlugin plugin;

    public AddPointLoader(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getKey() {
        return "zjobs_add_points";
    }

    @Override
    public Action load(String path, TypedMapAccessor accessor, File file) {
        int points = accessor.getInt("points");
        return new AddPointsAction(plugin, points);
    }
}
