package nl.nn.adapterframework.testutil.junit;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * TODO
 *
 * @author Niels Meijer
 */
@Documented
@Repeatable(WithLiquibase.LiquibaseFiles.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)

@Tag("database")
@Tag("liquibase")
//@TestInstance(Lifecycle.PER_METHOD)
@ExtendWith(JUnitLiquibaseExtension.class)
public @interface WithLiquibase {

	public static final String TEST_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";

	String file() default TEST_CHANGESET_PATH;
	String tableName() default "IBISSTORE";

	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.TYPE})
	public @interface LiquibaseFiles {
		WithLiquibase[] value();
	}
}
