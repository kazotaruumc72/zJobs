package fr.maxlego08.jobs.migrations;

import fr.maxlego08.jobs.api.Tables;
import fr.maxlego08.sarah.database.Migration;

public class CreatePlayerBoostLogMigration extends Migration {
    @Override
    public void up() {
        create(Tables.BOOST_LOGS, table -> {
            table.autoIncrement("id");
            table.uuid("unique_id").primary();
            table.longText("jobs").nullable();
            table.longText("actions").nullable();
            table.longText("targets").nullable();
            table.integer("boost_amount");
            table.decimal("experience_boost", 65, 2);
            table.decimal("money_boost", 65, 2);
            table.timestamp("started_at");
            table.timestamp("finished_at");
        });
    }
}
