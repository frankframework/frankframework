package org.frankframework.testutil.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Annotation to put on tests that need to manipulate the "current time" in date/time code. When you annotate a test class or test method with this annotation,
 * to actually manipulate time add a parameter of type {@link org.frankframework.util.TimeProvider.TimeTraveller}. Time can be set via the TimeTraveller.
 * <br/>
 * After the test is finished, time will automatically be reset to actual system time.
 * <br/>
 * Tests annotated as {@code WithTimeTravel} will run isolated from other tests, so not to influence time in other tests.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@ExtendWith(JUnitTimeTravelExtension.class)
@Isolated("Test manipulates current time, so should not be run concurrently with other tests")
public @interface WithTimeTravel {
}
