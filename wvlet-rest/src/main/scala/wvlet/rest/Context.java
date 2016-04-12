package wvlet.rest;

import java.lang.annotation.*;

/**
 * Used to inject HTTP request/response information as a method argument
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Context {
}
