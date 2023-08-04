package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class URLDataSourceFactory extends JndiDataSourceFactory {
	public static final String PRODUCT_KEY = "product";
	public static final String TEST_PEEK_KEY = "testPeek";
	private static final int DB_LOGIN_TIMEOUT = 1;
	private static List<String> availableDatasources = null;

	private static final Object[][] TEST_DATASOURCES = {
			// ProductName, Url, user, password, testPeekDoesntFindRecordsAlreadyLocked
			{ "H2",         "jdbc:h2:mem:test;LOCK_TIMEOUT=1000", null, null, false, "org.h2.jdbcx.JdbcDataSource" },
// Unit tests in 7.8 are not DB2 compatible for now.
//			{ "DB2",        "jdbc:db2://localhost:50000/testiaf", "testiaf_user", "testiaf_user00", false, "com.ibm.db2.jcc.DB2XADataSource" },
			{ "Oracle",     "jdbc:oracle:thin:@localhost:1521:XE", "testiaf_user", "testiaf_user00", false, "oracle.jdbc.xa.client.OracleXADataSource" },
			{ "MS_SQL",     "jdbc:sqlserver://localhost:1433;database=testiaf;lockTimeout=10000", 	"testiaf_user", "testiaf_user00", false, "com.microsoft.sqlserver.jdbc.SQLServerXADataSource" },
			{ "MySQL",      "jdbc:mysql://localhost:3307/testiaf?sslMode=DISABLED&disableMariaDbDriver=1&pinGlobalTxToPhysicalConnection=true&serverTimezone=Europe/Amsterdam", "testiaf_user", "testiaf_user00", true, "com.mysql.cj.jdbc.MysqlXADataSource" },
			{ "MariaDB",    "jdbc:mariadb://localhost:3306/testiaf?pinGlobalTxToPhysicalConnection=true&serverTimezone=Europe/Amsterdam", "testiaf_user", "testiaf_user00", false, "org.mariadb.jdbc.MariaDbDataSource" }, // can have only one entry per product key
			{ "PostgreSQL", "jdbc:postgresql://localhost:5432/testiaf", "testiaf_user", "testiaf_user00", true, "org.postgresql.xa.PGXADataSource" }
		};

	public URLDataSourceFactory() {
		if(availableDatasources == null) {
			availableDatasources = new ArrayList<>();
			DriverManager.setLoginTimeout(DB_LOGIN_TIMEOUT);

			for (Object[] datasource: TEST_DATASOURCES) {
				String product = (String)datasource[0];
				String url = (String)datasource[1];
				String userId = (String)datasource[2];
				String password = (String)datasource[3];
				boolean testPeek = (boolean)datasource[4];
				String xaImplClassName = (String)datasource[5];

				try { //Attempt to add the DataSource and skip it if it cannot be instantiated
					DataSource ds = createDataSource(product, url, userId, password, testPeek, xaImplClassName);
					// Check if we can make a connection
					if(validateConnection(product, ds)) {
						availableDatasources.add(product);
						log.info("adding DataSource {} for testing", product);
					}
				} catch (Exception e) {
					log.info("ignoring DataSource, cannot complete setup", e);
				}
			}
		}
	}

	private boolean validateConnection(String product, DataSource ds) {
		try(Connection ignored = ds.getConnection()) {
			return true;
		} catch (Throwable e) {
			log.warn("Cannot connect to [" + product + "], skipping:"+e.getMessage());
		}
		return false;
	}

	private DataSource namedDataSource(DataSource ds, String name, boolean testPeek) {
		return new DelegatingDataSource(ds) {
			@Override
			public String toString() {
				StringBuilder builder = new StringBuilder();
				builder.append(String.format("%s [%s]", PRODUCT_KEY, name));
				builder.append(String.format(" %s [%s]", TEST_PEEK_KEY, testPeek));
				return builder.toString();
			}
		};
	}

	protected DataSource createDataSource(String product, String url, String userId, String password, boolean testPeek, String implClassname) throws Exception {
		return new DriverManagerDataSource(url, userId, password);
	}

	@Override //fail fast
	public DataSource get(String jndiName, Properties jndiEnvironment) throws NamingException {
		if(!availableDatasources.contains(jndiName)) {
			throw new IllegalStateException("jndi ["+jndiName+"] not configured in test environment");
		}

		return super.get(jndiName, jndiEnvironment);
	}

	@Override
	protected CommonDataSource lookup(String jndiName, Properties jndiEnvironment) throws NamingException {
		for (Object[] datasource: TEST_DATASOURCES) {
			String product = (String)datasource[0];
			if(product.equals(jndiName)) {
				String url = (String)datasource[1];
				String userId = (String)datasource[2];
				String password = (String)datasource[3];
				boolean testPeek = (boolean)datasource[4];
				String xaImplClassName = (String)datasource[5];

				try {
					DataSource ds = createDataSource(product, url, userId, password, testPeek, xaImplClassName);
					return namedDataSource(ds, product, testPeek);
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		}
		return null;
	}

	@Override
	public List<String> getDataSourceNames() {
		return availableDatasources;
	}
}
