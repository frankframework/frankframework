package nl.nn.adapterframework.webcontrol.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Relation {
	String value() default "";
}
