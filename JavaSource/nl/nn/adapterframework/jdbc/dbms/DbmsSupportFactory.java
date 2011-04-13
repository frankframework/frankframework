/*
 * $Log: DbmsSupportFactory.java,v $
 * Revision 1.2  2011-04-13 08:44:10  L190409
 * Spring configurable DbmsSupport
 *
 * Revision 1.1  2011/03/16 16:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class DbmsSupportFactory implements IDbmsSupportFactory {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private final static String PRODUCT_NAME_ORACLE_="Oracle";
	private final static String PRODUCT_NAME_MSSQLSERVER="Microsoft SQL Server";

	private Properties dbmsSupportMap; 
	
	public IDbmsSupport getDbmsSupport(Connection conn) {
		String product;
		try {
			DatabaseMetaData md = conn.getMetaData();
			product=md.getDatabaseProductName();
		} catch (SQLException e1) {
			throw new RuntimeException("cannot obtain product from connection metadata", e1);
		}
		Properties supportMap=getDbmsSupportMap();
		if (supportMap!=null) {
			if (StringUtils.isEmpty(product)) {
				log.warn("no product found from connection metadata");
			} else {
				if (!supportMap.containsKey(product)) {
					log.warn("product ["+product+"] not configured in dbmsSupportMap");
				} else {
					String dbmsSupportClass=supportMap.getProperty(product);
					if (StringUtils.isEmpty(dbmsSupportClass)) {
						log.warn("product ["+product+"] configured empty in dbmsSupportMap");
					} else {
						try {
							if (log.isDebugEnabled()) log.debug("creating dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"]");
							return (IDbmsSupport)ClassUtils.newInstance(dbmsSupportClass);
						} catch (Exception e) {
							throw new RuntimeException("Cannot create dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"]",e);
						} 
					}
				}
			}				
		} else {
			log.warn("no dbmsSupportMap specified, reverting to built in types");
			if (PRODUCT_NAME_ORACLE_.equals(product)) {
				log.debug("Setting databasetype to ORACLE");
				return new OracleDbmsSupport();
			}
			if (PRODUCT_NAME_MSSQLSERVER.equals(product)) {
				log.debug("Setting databasetype to MSSQLSERVER");
				return new MsSqlServerDbmsSupport();
			}
		}
		log.debug("Setting databasetype to GENERIC, productName ["+product+"]");
		return new GenericDbmsSupport();
	}

	public Properties getDbmsSupportMap() {
		return dbmsSupportMap;
	}
	public void setDbmsSupportMap(Properties dbmsSupportMap) {
		this.dbmsSupportMap = dbmsSupportMap;
	}

}
