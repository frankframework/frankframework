package org.frankframework.testutil.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

/**
 * Run database enabled tests, allows the use of the {@link DatabaseTestEnvironment} object to be used in the test method.
 * Includes the {@link DataSourceArgumentSource} by default.
 *
 * @author Niels Meijer
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Nested

@DatabaseTestOptions
@DataSourceArgumentSource // automatically uses default datasource arguments
public @interface DatabaseTest {
}
