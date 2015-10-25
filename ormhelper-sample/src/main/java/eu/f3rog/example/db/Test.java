package eu.f3rog.example.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Class {@link Test}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-26
 */
@DatabaseTable
public class Test {

    @DatabaseField(id = true)
    int id;

}
