package com.hellblazer.anubis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Deployed annotation is used on a method that needs to be executed after
 * dependency injection is done, but before post construction startup. The
 * Deployed lifecycle state is missing from the Spring lifecycle and is used to
 * provide any post construction configuration that must occur before active
 * systems start up.
 * 
 * <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Deployed {

}
