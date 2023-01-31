package nl.nn.adapterframework.testutil;

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
	public static List<String> availableDatasources = null;

	private static final Object[][] TEST_DATASOURCES = {
			// ProductName, Url, user, password, testPeekDoesntFindRecordsAlreadyLocked
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
				//boolean testPeek = (boolean)datasource[4];
				//String xaImplClassName = (String)datasource[5];

				try { //Attempt to add the DataSource and skip it if it cannot be instantiated
					DataSource ds = new DriverManagerDataSource(url, userId, password); // do not use createDataSource here, as it has side effects in descender classes
					// Check if we can make a connection
					if(validateConnection(ds)) {
						availableDatasources.add(product);
					}
				} catch (Exception e) {
					log.info("ignoring DataSource, cannot complete setup", e);
					e.printStackTrace();
				}
			}
		}
	}

	private boolean validateConnection(DataSource ds) {
		try(Connection connection = ds.getConnection()) {
			return true;
		} catch (Throwable e) {
			log.warn("Cannot connect to ["+ds+"], skipping:"+e.getMessage());
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
				} catch (NamingException e) {
					throw e;
				} catch (Exception e) {
					NamingException ne = new NamingException("cannot lookup datasource");
					ne.initCause(e);
					ne.fillInStackTrace();
					throw ne;
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
