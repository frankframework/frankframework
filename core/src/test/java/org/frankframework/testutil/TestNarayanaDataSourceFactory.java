package org.frankframework.testutil;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.frankframework.jta.narayana.NarayanaDataSourceFactory;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;

public class TestNarayanaDataSourceFactory extends NarayanaDataSourceFactory {

	static {
		System.setProperty("transactionmanager.narayana.objectStoreType", VolatileStore.class.getCanonicalName());
	}

	@Override
	public DataSource getDataSource(String jndiName, Properties jndiEnvironment) throws NamingException {
		return super.getDataSource("jdbc/"+jndiName, jndiEnvironment);
	}

	private DataSource namedDataSource(DataSource ds, String name) {
		return new DelegatingDataSource(ds) {
			@Override
			public String toString() {
				return String.format("%s [%s] ", TestDataSourceFactory.PRODUCT_KEY, name.replaceAll("jdbc/", ""));
			}
		};
	}

	@Override
	protected DataSource createXADataSource(XADataSource xaDataSource, String product) {
		return namedDataSource(super.createXADataSource(XaDatasourceCommitStopper.augmentXADataSource(xaDataSource), product), product);
	}
}
