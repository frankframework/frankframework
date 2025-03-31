package org.frankframework.testutil.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that works in tandem with {@link DatabaseTest}, executes Liquibase before it runs the test and cleansup after the test has ran.
 * Is executed AFTER the @BeforeEach step.
 *
 * @author Niels Meijer
 */
@Documented
@Repeatable(WithLiquibase.LiquibaseFiles.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Tag("database")
@Tag("liquibase")
@ExtendWith(JUnitLiquibaseExtension.class)
public @interface WithLiquibase {

	String TEST_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";

	String file() default TEST_CHANGESET_PATH;
	String tableName() default "IBISSTORE";

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.TYPE})
	public @interface LiquibaseFiles {
		WithLiquibase[] value();
	}
}
