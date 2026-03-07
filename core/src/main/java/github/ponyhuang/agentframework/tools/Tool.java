package github.ponyhuang.agentframework.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class or method as a tool.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {

    /**
     * The name of the tool.
     * If not specified, the method name or class name is used.
     */
    String name() default "";

    /**
     * The description of what the tool does.
     */
    String description() default "";
}
