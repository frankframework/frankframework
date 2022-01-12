package nl.nn.adapterframework.testutil;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class URLDataSourceFactory extends JndiDataSourceFactory {
	public static final String PRODUCT_KEY = "product";
	public static final String TEST_PEEK_KEY = "testPeek";

	private static final Object[][] TEST_DATASOURCES = {
			// ProductName, Url, user, password, testPeekDoesntFindRecordsAlreadyLocked
			{ "H2",         "jdbc:h2:mem:test;LOCK_TIMEOUT=1000", null, null, false },
			{ "Oracle",     "jdbc:oracle:thin:@localhost:1521:ORCLCDB", 			"testiaf_user", "testiaf_user00", false }, 
			{ "MS_SQL",     "jdbc:sqlserver://localhost:1433;database=testiaf", 	"testiaf_user", "testiaf_user00", false }, 
			{ "MySQL",      "jdbc:mysql://localhost:3307/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00", true }, 
			//{ "MariaDB",   "jdbc:mariadb://localhost:3306/testiaf", 				"testiaf_user", "testiaf_user00", false }, // can have only one entry per product key
			{ "MariaDB",   "jdbc:mysql://localhost:3306/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00", false }, 
			{ "PostgreSQL", "jdbc:postgresql://localhost:5432/testiaf", 			"testiaf_user", "testiaf_user00", true }
		};

	public URLDataSourceFactory() {
		DriverManager.setLoginTimeout(1);
		for (Object[] datasource: TEST_DATASOURCES) {
			String product = (String)datasource[0];
			String url = (String)datasource[1];
			String userId = (String)datasource[2];
			String password = (String)datasource[3];
			boolean testPeek = (boolean)datasource[4];

			DriverManagerDataSource dataSource = new DriverManagerDataSource(url, userId, password) {
				@Override
				public String toString() { //Override toString so JunitTests are prefixed with the DataSource URL
					return "DataSource ["+product+"] url [" + getUrl()+"]";
				}
			};

			Properties properties = new Properties();
			properties.setProperty(PRODUCT_KEY, product);
			properties.setProperty(TEST_PEEK_KEY, ""+testPeek);
			dataSource.setConnectionProperties(properties);

			add(dataSource, product);
		}
	}

	@Override
	public DataSource get(String jndiName, Properties jndiEnvironment) throws NamingException {
		if(!objects.containsKey(jndiName)) {
			throw new IllegalStateException("jndi ["+jndiName+"] not configured in test environment");
		}

		return super.get(jndiName, jndiEnvironment);
	}

	public List<DataSource> getAvailableDataSources() {
		List<DataSource> availableDatasources = new ArrayList<>();
		for(String dataSourceName : getDataSourceNames()) {
			try {
				DataSource dataSource = getDataSource(dataSourceName);
				try(Connection connection = dataSource.getConnection()) {
					availableDatasources.add(dataSource);
				} catch (Exception e) {
					log.warn("Cannot connect to ["+dataSourceName+"], skipping:"+e.getMessage());
				}
			} catch (NamingException e) {
				fail(this.getClass().getSimpleName() +" should not look for DataSources in the JNDI");
			}
		}
		return availableDatasources;
	}
}
