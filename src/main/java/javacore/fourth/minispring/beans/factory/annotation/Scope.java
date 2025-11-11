package javacore.fourth.minispring.beans.factory.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {
    String value() default "singleton";
}
