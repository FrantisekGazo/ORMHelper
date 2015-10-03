package eu.f3rog.ormhelper;

import com.squareup.javapoet.ClassName;

/**
 * Class {@link EClass}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-26
 */
public enum EClass {

    Context(ClassName.get("android.content", "Context")),
    SQLiteDatabase(ClassName.get("android.database.sqlite", "SQLiteDatabase")),
    Log(ClassName.get("android.util", "Log")),

    OrmLiteSqliteOpenHelper(ClassName.get("com.j256.ormlite.android.apptools", "OrmLiteSqliteOpenHelper")),
    Dao(ClassName.get("com.j256.ormlite.dao", "Dao")),
    ConnectionSource(ClassName.get("com.j256.ormlite.support", "ConnectionSource")),
    TableUtils(ClassName.get("com.j256.ormlite.table", "TableUtils")),
    OrmLiteConfigUtil(ClassName.get("com.j256.ormlite.android.apptools", "OrmLiteConfigUtil"));

    private ClassName mClassName;

    EClass(ClassName className) {
        mClassName = className;
    }

    public ClassName getName() {
        return mClassName;
    }

}
