package org.frankframework.testutil.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("database")
@TestTemplate // Indicates it's not a test but provides the means to run a test
@ExtendWith(JUnitDatabaseExtension.class) // Turns it into a matrix test
@Timeout(value = 30, unit = TimeUnit.SECONDS) // No test should take longer then 30s...
@Documented
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface DatabaseTestOptions {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Parameter {
		/**
		 * Method that returns the index of the parameter in the array returned by the
		 * method annotated by <code>Parameters</code>. Index range must start at 0.
		 * Default value is 0.
		 *
		 * @return the index of the parameter.
		 */
		int value() default 0;
	}

	/**
	 * Ensure a clean database test environment is used
	 */
	boolean cleanupBeforeUse() default false;

	/**
	 * Ensure the database test environment is cleaned up after use
	 */
	boolean cleanupAfterUse() default false;
}
