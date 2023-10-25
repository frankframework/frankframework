package nl.nn.adapterframework.testutil.junit;

import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.AnnotationUtils;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JUnitLiquibaseExtension implements BeforeEachCallback, BeforeAllCallback, BeforeTestExecutionCallback {

	private static final String LIQUIBASE_CONFIGURATIONS = "LIQUIBASE_CONFIGURATIONS"; //With the idea you can populate this on class level
	private static final Namespace INSTANCE_NAMESPACE = Namespace.create(JUnitLiquibaseExtension.class);

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		Method templateMethod = context.getRequiredTestMethod();
		boolean isDatabaseTest = AnnotationUtils.findAnnotation(templateMethod, DatabaseTest.class).isPresent();
		if(!isDatabaseTest) {
			throw new JUnitException("Not a @DatabaseTest");
		}

		List<WithLiquibase> annotations = findRepeatableAnnotations(templateMethod, WithLiquibase.class);
		for(WithLiquibase liquibase : annotations) {
			String file = liquibase.file();
			URL url = JUnitLiquibaseExtension.class.getResource("/"+file);
			if(url == null) {
				throw new JUnitException("file ["+file+"] not found");
			}

			log.info("Running Liquibase file: {} tableName: {}", file, liquibase.tableName());
		}

		getStore(context).put(LIQUIBASE_CONFIGURATIONS, annotations);
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		System.out.println("test123");
	}

	private ExtensionContext.Store getStore(ExtensionContext context) {
		return context.getStore(Namespace.create(JUnitLiquibaseExtension.class, context.getRequiredTestMethod()));
	}

	@SuppressWarnings("unchecked")
	private List<WithLiquibase> getAnnotations(ExtensionContext context) {
		return getStore(context).get(LIQUIBASE_CONFIGURATIONS, List.class);
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		List<WithLiquibase> annotations = getAnnotations(context);
		if(annotations.isEmpty()) {
			return;
		}

		DatabaseTestEnvironment dbTestEnv = DatabaseTestInvocationContext.getDatabaseTestEnvironment(context);
		if(dbTestEnv == null) {
			throw new JUnitException("no DatabaseTestEnvironment found");
		}

		AtomicInteger argumentIndex = new AtomicInteger();
		for(WithLiquibase annotation : annotations) {
			String file = annotation.file();
			String tableName = annotation.tableName();

			System.setProperty("tableName", tableName);
			Liquibase liquibase = runMigrator(file, dbTestEnv, context);

			//Store every instance in the 'Store' so it's autoclosed after the test has ran, even when it fails.
			Store store = context.getStore(INSTANCE_NAMESPACE);
			store.put("liquibaseInstance#" + argumentIndex.incrementAndGet(), new CloseableArgument(liquibase));
		}
	}

	private Liquibase runMigrator(String changeLogFile, DatabaseTestEnvironment dbTestEnv, ExtensionContext context) throws Exception {
		Connection connection = dbTestEnv.createNonTransactionalConnection();
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
		Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
		liquibase.forceReleaseLocks();
		StringWriter out = new StringWriter(2048);
		liquibase.reportStatus(true, new Contexts(), out);
		log.info("Liquibase Database: {}, {}", liquibase.getDatabase().getDatabaseProductName(), liquibase.getDatabase().getDatabaseProductVersion());
		log.info("Liquibase Database connection: {}", liquibase.getDatabase());
		log.info("Liquibase changeset status:");
		log.info(out.toString());
		liquibase.update(new Contexts());
		return liquibase;
	}

	private static class CloseableArgument implements Store.CloseableResource {

		private final Liquibase liquibase;

		CloseableArgument(Liquibase liquibase) {
			this.liquibase = liquibase;
		}

		@Override
		public void close() throws Throwable {
			log.info("Closing Liquibase and releasing all connections");
			try {
				liquibase.dropAll();
			} catch(Exception e) {
				log.warn("Liquibase failed to drop all objects. Trying to rollback the changesets", e);
				liquibase.rollback(liquibase.getChangeSetStatuses(null).size(), null);
			}
			liquibase.close();
		}
	}
}
