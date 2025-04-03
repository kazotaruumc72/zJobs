package fr.maxlego08.jobs.migrations;

import fr.maxlego08.jobs.api.Tables;
import fr.maxlego08.sarah.database.Migration;

public class CreatePlayerBoostMigration extends Migration {
    @Override
    public void up() {
        create(Tables.BOOSTS, table -> {
            table.autoIncrement("id");
            table.uuid("unique_id").primary();
            table.string("job_id", 255).nullable();
            table.string("action_type", 255).nullable();
            table.integer("remaining_boost");
            table.decimal("experience_boost", 65, 2);
            table.decimal("money_boost", 65, 2);
            table.timestamps();
        });
    }
}
