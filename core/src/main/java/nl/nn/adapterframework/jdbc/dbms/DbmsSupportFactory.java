/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class DbmsSupportFactory implements IDbmsSupportFactory {
	protected Logger log = LogUtil.getLogger(this.getClass());


	private Properties dbmsSupportMap; 

	public IDbmsSupport getDbmsSupport(Connection conn) {
		String product;
		String productVersion;
		try {
			DatabaseMetaData md = conn.getMetaData();
			product = md.getDatabaseProductName();
			productVersion = md.getDatabaseProductVersion();
			
			log.debug("found product ["+product+"] productVersion ["+productVersion+"]");
		} catch (SQLException e1) {
			throw new RuntimeException("cannot obtain product from connection metadata", e1);
		}
		if (StringUtils.isEmpty(product)) {
			log.warn("no product found from connection metadata");
		} else {
			Properties supportMap=getDbmsSupportMap();
			if (supportMap==null) {
				log.debug("no dbmsSupportMap specified, reverting to built-in types");
			} else {
				if (!supportMap.containsKey(product)) {
					log.debug("product ["+product+"] not configured in dbmsSupportMap, will search in built-in types");
				} else {
					String dbmsSupportClass=supportMap.getProperty(product);
					if (StringUtils.isEmpty(dbmsSupportClass)) {
						log.warn("product ["+product+"] configured empty in dbmsSupportMap, will search in built-in types");
					} else {
						try {
							if (log.isDebugEnabled()) log.debug("creating dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"] productVersion ["+productVersion+"]");
							return (IDbmsSupport)ClassUtils.newInstance(dbmsSupportClass);
						} catch (Exception e) {
							throw new RuntimeException("Cannot create dbmsSupportClass ["+dbmsSupportClass+"] for product ["+product+"] productVersion ["+productVersion+"]",e);
						} 
					}
				}
			}
			return Dbms.findDbmsSupportByProduct(product, productVersion);
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
