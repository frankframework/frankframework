package nl.nn.adapterframework.testutil;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.nn.adapterframework.jndi.JndiDataSourceFactory;

public class URLDataSourceFactory extends JndiDataSourceFactory {

	public static final Object[][] TEST_DATASOURCES = {
			// ProductName, URL, username, password
			{ "H2",         "jdbc:h2:mem:test;LOCK_TIMEOUT=1000", null, null, false },
			{ "Oracle",     "jdbc:oracle:thin:@localhost:1521:ORCLCDB", 			"testiaf_user", "testiaf_user00" },
			{ "MS_SQL",     "jdbc:sqlserver://localhost:1433;database=testiaf", 	"testiaf_user", "testiaf_user00" },
			{ "MySQL",      "jdbc:mysql://localhost:3307/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00" },
			{ "MariaDB",    "jdbc:mariadb://localhost:3306/testiaf", 				"testiaf_user", "testiaf_user00" },
			{ "MariaDB",    "jdbc:mysql://localhost:3306/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00" },
			{ "PostgreSQL", "jdbc:postgresql://localhost:5432/testiaf", 			"testiaf_user", "testiaf_user00" }
		};

	public URLDataSourceFactory() {
		for (Object[] datasource: TEST_DATASOURCES) {
			String product = (String)datasource[0];
			String url = (String)datasource[1];
			String userId = (String)datasource[2];
			String password = (String)datasource[3];

			DriverManagerDataSource dataSource = new DriverManagerDataSource(url, userId, password);
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
}
