package nl.nn.adapterframework.testutil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import com.arjuna.ats.internal.jdbc.ConnectionManager;
import com.arjuna.ats.jdbc.TransactionalDriver;

public class NarayanaXADataSourceFactory extends URLXADataSourceFactory {

	@Override
	protected DataSource augmentXADataSource(XADataSource xaDataSource, String product) {
		return new DelegatingDataSource() { //Cannot use NarayanaDatasource as the PGSQL driver does not implement the Datasource interface
			@Override
			public Connection getConnection() throws SQLException {
				Properties properties = new Properties();
				properties.put(TransactionalDriver.XADataSource, xaDataSource);
				properties.setProperty(TransactionalDriver.poolConnections, "true");
				properties.setProperty(TransactionalDriver.maxConnections, "100");
				return ConnectionManager.create(null, properties);
			}

			@Override
			public String toString() {
				return product;
			}
		};
	}
}
