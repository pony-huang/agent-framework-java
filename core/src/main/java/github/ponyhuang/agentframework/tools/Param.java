package github.ponyhuang.agentframework.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to describe tool parameters.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {

    /**
     * The description of the parameter.
     */
    String description() default "";

    /**
     * Whether this parameter is required.
     */
    boolean required() default true;

    /**
     * The default value of the parameter.
     */
    String defaultValue() default "";
}
