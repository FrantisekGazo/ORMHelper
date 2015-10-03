# OrmHelper 
Android library for generating [OrmLite](http://ormlite.com/) Helper and ConfiUtil classes.
This library generates classes during compile-time using Java Annotation Processor. 

Helper classes provide:
- ORMLite [DAO](http://ormlite.com/javadoc/ormlite-core/com/j256/ormlite/dao/Dao.html) objects 
- methods for creating, upgrading and clearing the all database tables

# Usage
There are 2 important annotations:
- ``` @Helper ```
- ``` @OnUpgrade ```
 
# @Helper
This annotation can be used to annotate a class. Annotated class will be then used to upgrade database if ``` dropOnUpgrade ``` is ``` false ``` and contains some ``` @OnUpgrade ``` method.

### Mandatory fields:
- ``` name ``` = Database name.
- ``` tables ``` = List of classes representing database tables. Each class has to be annotated with [@DatabaseTable] (http://ormlite.com/javadoc/ormlite-core/com/j256/ormlite/table/DatabaseTable.html) (part of ORMLite library) and has to contain attribute annotated with [@DatabaseField] (http://ormlite.com/javadoc/ormlite-core/com/j256/ormlite/field/DatabaseField.html) (also part of ORMLite library) representing table id.
 

### Optional fields:
- ``` version ```  (``` 0 ``` by default) = Database version.
- ``` dropOnUpgrade ``` (``` false ``` by default) = If set to true, then all tables will be droped and recreated on database upgrade. (Useful for app development) Note: all ``` @OnUpgrade ``` methods will be ignored.
- ``` withConfigUtil ``` (``` false ``` by default) = If set to true, then [ConfigUtil](http://ormlite.com/javadoc/ormlite-android/com/j256/ormlite/android/apptools/OrmLiteConfigUtil.html) class will be also generated. This class provides easy way to write ORMLite configuration file. 

#### Example:

```Java

@Helper(
        name = "test",
        version = 3,
        tables = {
                Test.class,
                Question.class,
                Answer.class,
        },
        dropOnUpgrade = false,
        withConfigUtil = true
)
public class MyUpgrader {
}

```

# @OnUpgrade
This annotation can be used to annotate methods inside class annotated with ``` @Helper ```. Annotated method has to have 2 parameters - [SQLiteDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html) and [ConnectionSource](http://ormlite.com/javadoc/ormlite-android/com/j256/ormlite/android/AndroidConnectionSource.html).

- ``` from ``` = optional field. Number of database version from which database will be upgraded.
- ``` to ``` = mandatory field. Number of database version to which database will be upgraded.

#### Example:

```Java

    @OnUpgrade(from = 1, to = 2)
    public void upgrade1To2(SQLiteDatabase database, ConnectionSource connectionSource) {
        // do something in database
    }

```
