package fr.maxlego08.jobs.zmenu.loader;

import fr.maxlego08.jobs.JobsPlugin;
import fr.maxlego08.jobs.zmenu.actions.AddPointsAction;
import fr.maxlego08.menu.api.loader.ActionLoader;
import fr.maxlego08.menu.api.requirement.Action;
import fr.maxlego08.menu.api.utils.TypedMapAccessor;

import java.io.File;

/**
 * Loader for the {@code zjobs_add_points} menu action.
 */
public class AddPointLoader extends ActionLoader {

    private final JobsPlugin plugin;

    /**
     * Constructs a new {@code AddPointLoader}.
     *
     * @param plugin the jobs plugin instance.
     */
    public AddPointLoader(JobsPlugin plugin) {
        super("zjobs_add_points", "zjobs add points");
        this.plugin = plugin;
    }

    @Override
    public Action load(String path, TypedMapAccessor accessor, File file) {
        String points = accessor.getString("points");
        return new AddPointsAction(plugin, points);
    }
}
