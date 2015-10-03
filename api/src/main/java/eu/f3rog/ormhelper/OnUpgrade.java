package eu.f3rog.ormhelper;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
public @interface OnUpgrade {

    int UNDEFINED = -1;

    /**
     * <p>
     * Version from which this upgrade should be made.
     * </p>
     */
    int fromVersion() default UNDEFINED;

    /**
     * <p>
     * Version to which this upgrade should be made.
     * </p>
     */
    int toVersion();

}