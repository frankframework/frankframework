package org.frankframework.testutil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.jndi.ResourceBasedObjectFactory;

public class FindAvailableDataSources {
	private static final Logger LOG = LogManager.getLogger(FindAvailableDataSources.class);

	private static final int DB_LOGIN_TIMEOUT = 1;
	private static List<String> availableDataSources = null;
	private static final ResourceBasedObjectFactory objectLocator = new ResourceBasedObjectFactory();

	private static final String[] TEST_DATASOURCES = { "H2", "DB2", "Oracle", "MS_SQL", "MySQL", "MariaDB", "PostgreSQL" };

	public FindAvailableDataSources() {
		getAvailableDataSources();
	}

	public static List<String> getAvailableDataSources() {
		if (availableDataSources == null) {
			availableDataSources = findAvailableDataSources();
		}
		return availableDataSources;
	}

	private static List<String> findAvailableDataSources() {
		List<String> availableDatasources = new ArrayList<>();
		DriverManager.setLoginTimeout(DB_LOGIN_TIMEOUT);

		try {
			objectLocator.afterPropertiesSet();
		} catch (Exception e) {
			throw new IllegalArgumentException("unable to read yaml?");
		}

		for (String product: TEST_DATASOURCES) {

			try { //Attempt to add the DataSource and skip it if it cannot be instantiated
				CommonDataSource ds = objectLocator.lookup("jdbc/"+product, null, CommonDataSource.class); // do not use createDataSource here, as it has side effects in descender classes
				// Check if we can make a connection
				if(ds instanceof DataSource && validateConnection(product, (DataSource)ds)) {
					availableDatasources.add(product);
					LOG.info("adding DataSource {} for testing", product);
				}
			} catch (Exception e) {
				LOG.info("ignoring DataSource for [" + product + "], cannot complete setup", e);
			}
		}
		return availableDatasources;
	}

	private static boolean validateConnection(String product, DataSource ds) {
		try(Connection ignored = ds.getConnection()) {
			return true;
		} catch (Throwable e) {
			LOG.warn("Cannot connect to [{}], skipping: {}", product, e.getMessage());
		}
		return false;
	}
}
