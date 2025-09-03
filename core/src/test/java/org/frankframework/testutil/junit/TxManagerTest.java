package org.frankframework.testutil.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Annotation to add on tests that should run with all types of transaction managers, both JTA and Non-JTA
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(Lifecycle.PER_CLASS)
@Nested
@DatabaseTestOptions

// Adds these two ArgumentSources for tests with the database + transaction-manager
@DataSourceArgumentSource
@NarayanaArgumentSource
public @interface TxManagerTest {
}
