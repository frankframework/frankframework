package nl.nn.adapterframework.testutil.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Run database enabled tests, allows the use of the {@link DatabaseTestEnvironment} object to be used in the test method.
 *
 * @author Niels Meijer
 */
@Tag("database")
@TestTemplate // Indicates it's not a test but provides the means to run a test
@ArgumentsSource(JUnitDatabaseSource.class) // Creates all arguments
@ExtendWith(JUnitDatabaseExtension.class) // Turns it into a matrix test

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(Lifecycle.PER_CLASS)
public @interface DatabaseTest {

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
	 * AutoClose after tests has run
	 */
	boolean cleanupBeforeUse() default false;

	/**
	 * AutoClose after tests has run
	 */
	boolean cleanupAfterUse() default false;
}
