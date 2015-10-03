package eu.f3rog.ormhelper;

import com.google.common.base.Joiner;
import com.google.common.truth.Truth;
import com.google.testing.compile.JavaFileObjects;
import com.google.testing.compile.JavaSourcesSubjectFactory;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * Class {@link HelperProcessorTest}.
 *
 * @author Frantisek Gazo
 * @version 2015-09-20
 */
public class HelperProcessorTest {

    private JavaFileObject classFile(String packageName, String className) {
        return JavaFileObjects.forSourceString(String.format("%s.%s", packageName, className),
                Joiner.on('\n').join(
                        String.format("package %s;", packageName),
                        String.format("public class %s {}", className)
                )
        );
    }

    private JavaFileObject tableClass1() {
        return JavaFileObjects.forSourceString("com.example.model.TableClass1",
                Joiner.on('\n').join(
                        "package com.example.model;",
                        "",
                        "import com.example.model.BaseClass;",
                        "import com.j256.ormlite.table.DatabaseTable;",
                        "",
                        "@DatabaseTable",
                        "public class TableClass1 extends BaseClass {",
                        "",
                        "}"
                )
        );
    }

    private JavaFileObject baseClass() {
        return JavaFileObjects.forSourceString("com.example.model.BaseClass",
                Joiner.on('\n').join(
                        "package com.example.model;",
                        "",
                        "import com.j256.ormlite.field.DatabaseField;",
                        "",
                        "public class BaseClass {",
                        "",
                        "    @DatabaseField(id = true)",
                        "    private String id;",
                        "",
                        "}"
                )
        );
    }

    private Iterable<JavaFileObject> files(JavaFileObject... f) {
        List<JavaFileObject> files = new ArrayList<>();
        files.add(baseClass());
        files.add(tableClass1());
        files.addAll(Arrays.asList(f));
        return files;
    }

    @Test
    public void missingEmptyConstructor() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {}",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "   public MyDatabaseHelper(int i) { }",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("must have empty constructor");
    }

    @Test
    public void missingDatabaseTableAnnotation() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import com.example.model.Missing;",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {",
                        "       Missing.class",
                        "   }",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "}"
                )
        );

        JavaFileObject table = JavaFileObjects.forSourceString("com.example.model.Missing",
                Joiner.on('\n').join(
                        "package com.example.model;",
                        "",
                        "public class Missing {",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(table, helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("@Helper tables must contain classes annotated with @DatabaseTable");
    }

    @Test
    public void noOnUpgradeParams() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {}",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "   @OnUpgrade(toVersion = 1)",
                        "   public void up() {}",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("must have 2 parameters");
    }

    @Test
    public void oneOnUpgradeParam() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {}",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "   @OnUpgrade(toVersion = 1)",
                        "   public void up(int i) {}",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("must have 2 parameters");
    }

    @Test
    public void wrong1stOnUpgradeParam() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import android.database.sqlite.SQLiteDatabase;",
                        "import com.j256.ormlite.support.ConnectionSource;",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {}",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "   @OnUpgrade(toVersion = 1)",
                        "   public void up(Object o1, Object o2) {}",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("must have 1st parameter of type");
    }

    @Test
    public void wrong2ndOnUpgradeParam() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.MyDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import android.database.sqlite.SQLiteDatabase;",
                        "import com.j256.ormlite.support.ConnectionSource;",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   name = \"my_database.db\",",
                        "   tables = {}",
                        ")",
                        "public class MyDatabaseHelper {",
                        "",
                        "   @OnUpgrade(toVersion = 1)",
                        "   public void up(SQLiteDatabase database, Object o) {}",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .failsToCompile()
                .withErrorContaining("must have 2nd parameter of type");
    }

    @Test
    public void dropOnUpgrade() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.CustomerDatabase",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import com.example.model.TableClass1;",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   version = 3,",
                        "   name = \"customer\",",
                        "   tables = {",
                        "       TableClass1.class",
                        "   },",
                        "   dropOnUpgrade = true",
                        ")",
                        "public class CustomerDatabase {",
                        "}"
                )
        );

        JavaFileObject expectedFile = JavaFileObjects.forSourceString("com.example.CustomerDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import android.content.Context;",
                        "import android.database.sqlite.SQLiteDatabase;",
                        "import android.util.Log;",
                        "import com.example.model.TableClass1;",
                        "import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;",
                        "import com.j256.ormlite.dao.Dao;",
                        "import com.j256.ormlite.support.ConnectionSource;",
                        "import com.j256.ormlite.table.TableUtils;",
                        "import java.lang.Override;",
                        "import java.lang.String;",
                        "import java.sql.SQLException;",
                        "",
                        "public final class CustomerDatabaseHelper extends OrmLiteSqliteOpenHelper {",
                        "",
                        "   private Dao<TableClass1, String> mTableClass1Dao;",
                        "",
                        "   public Gen_MyDatabaseHelper(Context context) {",
                        "       super(context, \"customer.db\", null, 3);",
                        "   }",
                        "",
                        "   @Override",
                        "   public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "       Log.i(CustomerDatabase.class.getName(), \"onCreate\");",
                        "       try {",
                        "               TableUtils.createTable(connectionSource, TableClass1.class);",
                        "       }",
                        "       catch (SQLException e) {",
                        "               Log.e(CustomerDatabase.class.getName(), \"Can't create database tables.\", e);",
                        "               throw new RuntimeException(e);",
                        "       }",
                        "   }",
                        "",
                        "   @Override",
                        "   public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {",
                        "       dropTables(database, connectionSource);",
                        "       onCreate(database, connectionSource);",
                        "   }",
                        "",
                        "   private void dropTables(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "       Log.i(CustomerDatabase.class.getName(), \"dropTables\");",
                        "       try {",
                        "           TableUtils.dropTable(connectionSource, TableClass1.class, true);",
                        "       }",
                        "       catch (SQLException e) {",
                        "           Log.e(CustomerDatabase.class.getName(), \"Can't drop database tables.\", e);",
                        "       }",
                        "   }",
                        "",
                        "   public void clearTables() {",
                        "       Log.i(CustomerDatabase.class.getName(), \"clearTables\");",
                        "       try {",
                        "           connectionSource.getReadWriteConnection();",
                        "           TableUtils.clearTable(connectionSource, TableClass1.class);",
                        "       }",
                        "       catch (SQLException e) {",
                        "           Log.e(CustomerDatabase.class.getName(), \"Can't clear database tables.\", e);",
                        "       }",
                        "   }",
                        "",
                        "   @Override",
                        "   public void close() {",
                        "       super.close();",
                        "       mTableClass1Dao = null;",
                        "   }",
                        "",
                        "   public Dao<TableClass1, String> getTableClass1Dao() throws SQLException {",
                        "       if (mTableClass1Dao == null)  {",
                        "           mTableClass1Dao = getDao(TableClass1.class);",
                        "       }",
                        "       return mTableClass1Dao;",
                        "   }",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedFile);
    }

    @Test
    public void customUpgrade() {
        JavaFileObject helperClass = JavaFileObjects.forSourceString("com.example.WhateverDatabase",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import android.database.sqlite.SQLiteDatabase;",
                        "import com.example.model.TableClass1;",
                        "import com.j256.ormlite.support.ConnectionSource;",
                        "import eu.f3rog.ormhelper.Helper;",
                        "import eu.f3rog.ormhelper.OnUpgrade;",
                        "",
                        "@Helper(",
                        "   version = 3,",
                        "   name = \"whatever\",",
                        "   tables = {",
                        "       TableClass1.class",
                        "   }",
                        ")",
                        "public class WhateverDatabase {",
                        "",
                        "   @OnUpgrade(fromVersion = 1, toVersion = 3)",
                        "   public void up1to2(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "   }",
                        "",
                        "   @OnUpgrade(fromVersion = 2, toVersion = 3)",
                        "   public void up2to3(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "   }",
                        "",
                        "   @OnUpgrade(toVersion = 4)",
                        "   public void upto4(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "   }",
                        "",
                        "   @OnUpgrade(toVersion = 5)",
                        "   public void upto5(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "   }",
                        "",
                        "   @OnUpgrade(toVersion = 6)",
                        "   public void upto6(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "   }",
                        "",
                        "}"
                )
        );

        JavaFileObject expectedFile = JavaFileObjects.forSourceString("com.example.WhateverDatabaseHelper",
                Joiner.on('\n').join(
                        "package com.example;",
                        "",
                        "import android.content.Context;",
                        "import android.database.sqlite.SQLiteDatabase;",
                        "import android.util.Log;",
                        "import com.example.model.TableClass1;",
                        "import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;",
                        "import com.j256.ormlite.dao.Dao;",
                        "import com.j256.ormlite.support.ConnectionSource;",
                        "import com.j256.ormlite.table.TableUtils;",
                        "import java.lang.Override;",
                        "import java.lang.String;",
                        "import java.sql.SQLException;",
                        "",
                        "public final class WhateverDatabaseHelper extends OrmLiteSqliteOpenHelper {",
                        "",
                        "   private Dao<TableClass1, String> mTableClass1Dao;",
                        "",
                        "   public Gen_MyDatabaseHelper(Context context) {",
                        "       super(context, \"whatever.db\", null, 3);",
                        "   }",
                        "",
                        "   @Override",
                        "   public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {",
                        "       Log.i(WhateverDatabase.class.getName(), \"onCreate\");",
                        "       try {",
                        "               TableUtils.createTable(connectionSource, TableClass1.class);",
                        "       }",
                        "       catch (SQLException e) {",
                        "           Log.e(WhateverDatabase.class.getName(), \"Can't create database tables.\", e);",
                        "           throw new RuntimeException(e);",
                        "       }",
                        "   }",
                        "",
                        "   @Override",
                        "   public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {",
                        "       WhateverDatabase upgradeHelper = new WhateverDatabase();",
                        "       int version = oldVersion;",
                        "       if (version == 1) {",
                        "           upgradeHelper.up1to2(database, connectionSource);",
                        "           version = 3;",
                        "       }",
                        "       if (version == 2) {",
                        "           upgradeHelper.up2to3(database, connectionSource);",
                        "           version = 3;",
                        "       }",
                        "       if (version <= 4) {",
                        "            upgradeHelper.upto4(database, connectionSource);",
                        "            version = 4;",
                        "       }",
                        "       if (version <= 5) {",
                        "           upgradeHelper.upto5(database, connectionSource);",
                        "           version = 5;",
                        "       }",
                        "       if (version <= 6) {",
                        "           upgradeHelper.upto6(database, connectionSource);",
                        "           version = 6;",
                        "       }",
                        "   }",
                        "",
                        "   public void clearTables() {",
                        "       Log.i(WhateverDatabase.class.getName(), \"clearTables\");",
                        "       try {",
                        "           connectionSource.getReadWriteConnection();",
                        "           TableUtils.clearTable(connectionSource, TableClass1.class);",
                        "       }",
                        "       catch (SQLException e) {",
                        "           Log.e(WhateverDatabase.class.getName(), \"Can't clear database tables.\", e);",
                        "       }",
                        "   }",
                        "",
                        "   @Override",
                        "   public void close() {",
                        "       super.close();",
                        "       mTableClass1Dao = null;",
                        "   }",
                        "",
                        "   public Dao<TableClass1, String> getTableClass1Dao() throws SQLException {",
                        "       if (mTableClass1Dao == null)  {",
                        "           mTableClass1Dao = getDao(TableClass1.class);",
                        "       }",
                        "       return mTableClass1Dao;",
                        "   }",
                        "",
                        "}"
                )
        );

        Truth.assert_()
                .about(JavaSourcesSubjectFactory.javaSources())
                .that(files(helperClass))
                .processedWith(new HelperProcessor())
                .compilesWithoutError()
                .and()
                .generatesSources(expectedFile);
    }

}
