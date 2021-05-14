package nl.nn.adapterframework.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FrankDocGroup {
	String groupName();
	int groupOrder() default Integer.MAX_VALUE;
}
