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
    // make enable true to enable the custom claim mapping class for the issuer.
    boolean enabled() default false;
    // Name of the class. "ex:CustomJWTTransformer"
    String name();
    // Issuer. ex: "https://localhost:9443/oauth2/token"
    String issuer();
}
