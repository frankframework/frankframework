package nl.nn.adapterframework.testutil;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

public abstract class URLXADataSourceFactory extends URLDataSourceFactory {

	@Override
	protected DataSource createDataSource(String product, String url, String userId, String password, boolean testPeek, String implClassname) {
		try {
			XADataSource xaDataSource = (XADataSource)Class.forName(implClassname).newInstance();
			BeanUtils.setProperty(xaDataSource, "URL", url);
			if (StringUtils.isNotEmpty(userId)) BeanUtils.setProperty(xaDataSource, "user", userId);
			if (StringUtils.isNotEmpty(password)) BeanUtils.setProperty(xaDataSource, "password", password);
			DataSource ds = augmentXADataSource(xaDataSource, product);

			return new TransactionAwareDataSourceProxy(ds) {
				@Override
				public String toString() {
					return product;
				}
			};
		} catch (Exception e) {
			log.info("ignoring DataSource, cannot complete setup", e);
			e.printStackTrace();
			return null;
		}
	}

	protected abstract DataSource augmentXADataSource(XADataSource xaDataSource, String product);
	
	private void testClassMethods() {
		org.h2.jdbcx.JdbcDataSource h2 = null; 
		oracle.jdbc.xa.client.OracleXADataSource oracle = null;
		com.microsoft.sqlserver.jdbc.SQLServerXADataSource mssql = null;
		com.mysql.cj.jdbc.MysqlXADataSource mysql = null;
		org.postgresql.xa.PGXADataSource postgres = null;

		h2.setUrl("x");
		h2.setURL("x");
		h2.setUser("x");
		h2.setPassword("x");

		//oracle.setUrl("x");
		oracle.setURL("x");
		oracle.setUser("x");
		oracle.setPassword("x");

		//mssql.setUrl("x");
		mssql.setURL("x");
		mssql.setUser("x");
		mssql.setPassword("x");

		mysql.setUrl("x");
		mysql.setURL("x");
		mysql.setUser("x");
		mysql.setPassword("x");

		postgres.setUrl("x");
		postgres.setURL("x");
		postgres.setUser("x");
		postgres.setPassword("x");
	}
	
}
