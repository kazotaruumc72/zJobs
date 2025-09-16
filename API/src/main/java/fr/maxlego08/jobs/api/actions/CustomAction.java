package fr.maxlego08.jobs.api.actions;

import fr.maxlego08.jobs.api.enums.JobActionType;
import org.bukkit.Material;

public class CustomAction extends ActionInfo<String>  {
    public CustomAction(JobActionType actionType, String value) {
        super(actionType, value);
    }
}
