package eu.f3rog.example.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Class {@link A2}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-26
 */
@DatabaseTable
public class A2 {

    @DatabaseField(id = true)
    float id;
}
