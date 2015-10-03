package eu.f3rog.ormhelper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
public @interface Helper {

    /**
     * <p>
     * Database version.
     * </p>
     * <p>
     * <b> NOTE: </b>
     * Default value is 0.
     * </p>
     */
    int version() default 1;

    /**
     * <p>
     * Database name.
     * </p>
     * <p>
     * <b> NOTE: </b>
     * Specify name without special characters. e.g. {@code "example"}.
     * Generated classes will use this name and will be called {@code ExampleDatabaseHelper} and {@code ExampleDatabaseConfigUtil}.
     * </p>
     */
    String name();

    /**
     * <p>
     * Array of classes representing database tables.
     * </p>
     * <p>
     * <b> WARNING: </b>
     * Each class has to be annotated with ORMLite annotation {@code @DatabaseTable}.
     * </p>
     */
    Class<?>[] tables();

    /**
     * <p>
     * If set to {@code true}, database tables will be dropped and recreated on database upgrade. (Useful for development phase)
     * </p>
     * <p>
     * <b> WARNING: </b>
     * If set to {@code true}, all methods annotated with @{@link OnUpgrade} will be ignored.
     * </p>
     */
    boolean dropOnUpgrade() default false;

    /**
     * <p>
     * If set to {@code true}, ConfigUtil class will be generated. This class can be used to generate ORMLite config file for this database.
     * </p>
     * <p>
     * <b> WARNING: </b>
     * If set to {@code true}, you will have to run the generated class in order to generate config file (do not move the generated config file). Otherwise database won’t be created.
     * </p>
     */
    boolean withConfigUtil() default false;

}
