package org.wso2.micro.gateway.enforcer.security.jwt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation to retrieve data about the jwt transformer class.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface JwtTransformerAnnotation {
    boolean enabled() default false;
    String name();
    String issuer();
}
