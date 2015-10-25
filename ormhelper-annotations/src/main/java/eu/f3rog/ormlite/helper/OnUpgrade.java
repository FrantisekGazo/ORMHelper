package eu.f3rog.ormlite.helper;

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
    int from() default UNDEFINED;

    /**
     * <p>
     * Version to which this upgrade should be made.
     * </p>
     */
    int to();

}
