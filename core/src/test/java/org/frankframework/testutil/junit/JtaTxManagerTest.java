package org.frankframework.testutil.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import org.frankframework.jta.xa.XaResourceObserverFactory;

/**
 * Annotation to add on tests that should only run with JTA transaction managers, but not with the DataSource Transaction Manager
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Nested

@DatabaseTestOptions

// Argument sources should add all JTA transaction managers supported by the Frank!Framework; currently only Narayana
@NarayanaArgumentSource
public @interface JtaTxManagerTest {

	Class<? extends XaResourceObserverFactory> resourceObserverFactory() default XaResourceObserverFactory.class;
}
