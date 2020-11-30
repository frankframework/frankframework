/*
   Copyright 2020 WeAreFrank!

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

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public enum Dbms {

	NONE("none", null),
	GENERIC("generic", GenericDbmsSupport.class),
	ORACLE("Oracle", OracleDbmsSupport.class),
	MSSQL("MS_SQL", "Microsoft SQL Server", MsSqlServerDbmsSupport.class),
	DB2("DB2", GenericDbmsSupport.class),
	H2("H2", H2DbmsSupport.class),
	MYSQL("MySQL", MySqlDbmsSupport.class),
	MARIADB("MariaDB", MariaDbDbmsSupport.class),
	POSTGRESQL("PostgreSQL", PostgresqlDbmsSupport.class);

	protected static Logger log = LogUtil.getLogger(Dbms.class);

	private String key;
	private String productName;
	private Class dbmsSupportClass;
	
	private Dbms(String key, Class dbmsSupportClass) {
		this(key, key, dbmsSupportClass);
	}
	private Dbms(String key, String productName, Class dbmsSupportClass) {
		this.key = key;
		this.productName = productName;
		this.dbmsSupportClass = dbmsSupportClass;
	}

	public String getKey() {
		return key;
	}

	public String getProductName() {
		return productName;
	}
	
	public static IDbmsSupport findDbmsSupportByProduct(String product, String productVersion) {
		if (MYSQL.getProductName().equals(product) && productVersion.contains("MariaDB")) {
			log.debug("Setting databasetype to MARIADB (using MySQL driver)");
			return new MariaDbDbmsSupport();
		}
		for (Dbms dbms: values()) {
			if (dbms.getProductName().equals(product)) {
				log.debug("Setting databasetype to ["+dbms+"]");
				IDbmsSupport result;
				try {
					result = (IDbmsSupport)dbms.dbmsSupportClass.newInstance();
					if (result != null) {
						log.debug("Returning built-in DBMS ["+dbms+"] found for product ["+product+"]");
						return result;
					}
					log.warn("No DbmsSupport configured for built-in DBMS ["+dbms+"] found for product ["+product+"]");
				} catch (IllegalAccessException | InstantiationException e) {
					log.warn("Could not instantiate DbmsSupport for DBMS ["+dbms+"] found for product ["+product+"]", e);
				}
			}
		}
		log.debug("Returning GenericDbmsSupport for product ["+product+"]");
		return new GenericDbmsSupport();
		
	}
	
	public IDbmsSupport getDbmsSupport() throws IllegalAccessException, InstantiationException {
		if (dbmsSupportClass==null) {
			return null;
		}
		return (IDbmsSupport)dbmsSupportClass.newInstance();
	}
}
