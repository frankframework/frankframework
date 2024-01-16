package org.frankframework.testutil;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.jta.xa.XaDatasourceCommitStopper;
import org.mariadb.jdbc.MariaDbDataSource;

import com.ibm.db2.jcc.DB2XADataSource;

public abstract class URLXADataSourceFactory extends URLDataSourceFactory {

	@Override
	protected DataSource createDataSource(String product, String url, String userId, String password, boolean testPeek, String implClassname) throws Exception {
		XADataSource xaDataSource = (XADataSource)Class.forName(implClassname).newInstance();
		if (xaDataSource instanceof DB2XADataSource) {
			DB2XADataSource db2 = (DB2XADataSource) xaDataSource;
			db2.setServerName("localhost");
			db2.setPortNumber(50000);
			db2.setDatabaseName("testiaf");
			db2.setDriverType(4);
		} else if (xaDataSource instanceof MariaDbDataSource) {
			BeanUtils.setProperty(xaDataSource, "url", url);
		} else {
			BeanUtils.setProperty(xaDataSource, "URL", url);
		}
		if (StringUtils.isNotEmpty(userId)) BeanUtils.setProperty(xaDataSource, "user", userId);
		if (StringUtils.isNotEmpty(password)) BeanUtils.setProperty(xaDataSource, "password", password);
		xaDataSource = XaDatasourceCommitStopper.augmentXADataSource(xaDataSource);
		return augmentXADataSource(xaDataSource, product);
	}

	@Override
	protected DataSource augmentDatasource(CommonDataSource xaDataSource, String product) {
		return super.augmentDatasource(xaDataSource, product);
	}

	protected abstract DataSource augmentXADataSource(XADataSource xaDataSource, String product);

	@SuppressWarnings({ "unused", "null" }) //only used to verify that all datasources use the same setters
	private void testClassMethods() throws Exception {
		org.h2.jdbcx.JdbcDataSource h2 = null;
		DB2XADataSource db2 = null;
		oracle.jdbc.xa.client.OracleXADataSource oracle = null;
		com.microsoft.sqlserver.jdbc.SQLServerXADataSource mssql = null;
		com.mysql.cj.jdbc.MysqlXADataSource mysql = null;
		org.postgresql.xa.PGXADataSource postgres = null;
		MariaDbDataSource mariadb = null;

		h2.setUrl("x");
		h2.setURL("x");
		h2.setUser("x");
		h2.setPassword("x");

		//db2.setUrl("x");
		//db2.setURL("x"); --> cannot set URL on DB2 datasource
		db2.setUser("x");
		db2.setPassword("x");

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

		mariadb.setUrl("x");
		mariadb.setUser("x");
		mariadb.setPassword("x");

		postgres.setUrl("x");
		postgres.setURL("x");
		postgres.setUser("x");
		postgres.setPassword("x");
	}
}
