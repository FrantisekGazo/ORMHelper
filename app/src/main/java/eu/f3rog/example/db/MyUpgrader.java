package eu.f3rog.example.db;

import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.support.ConnectionSource;

import eu.f3rog.ormhelper.Helper;
import eu.f3rog.ormhelper.OnUpgrade;

/**
 * Class {@link MyUpgrader}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-26
 */
@Helper(
        name = "test",
        version = 2,
        tables = {
                Test.class,
                Question.class,
                Answer.class,
                A1.class,
                A2.class,
        },
        dropOnUpgrade = false,
        withConfigUtil = true
)
public class MyUpgrader {

    @OnUpgrade(fromVersion = 1, toVersion = 2)
    public void to2(SQLiteDatabase database, ConnectionSource connectionSource) {}

}
