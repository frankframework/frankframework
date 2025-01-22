/*
   Copyright 2020-2023 WeAreFrank!

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
package org.frankframework.dbms;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;

@Log4j2
public enum Dbms {

	NONE("none", null),
	GENERIC("generic", GenericDbmsSupport.class),
	ORACLE("Oracle", OracleDbmsSupport.class),
	MSSQL("MS_SQL", "Microsoft SQL Server", MsSqlServerDbmsSupport.class),
	DB2("DB2", Db2DbmsSupport.class),
	H2("H2", H2DbmsSupport.class),
	MYSQL("MySQL", MySqlDbmsSupport.class),
	MARIADB("MariaDB", MariaDbDbmsSupport.class),
	POSTGRESQL("PostgreSQL", PostgresqlDbmsSupport.class);

	private @Getter String key;
	private @Getter String productName;
	private Class<? extends IDbmsSupport> dbmsSupportClass;

	private Dbms(String key, Class<? extends IDbmsSupport> dbmsSupportClass) {
		this(key, key, dbmsSupportClass);
	}

	private Dbms(String key, String productName, Class<? extends IDbmsSupport> dbmsSupportClass) {
		this.key = key;
		this.productName = productName;
		this.dbmsSupportClass = dbmsSupportClass;
	}

	public static IDbmsSupport findDbmsSupportByProduct(String product, String productVersion) {
		if (productVersion.contains("MariaDB")) {
			if (MYSQL.getProductName().equals(product)) {
				log.debug("Setting databasetype to MARIADB (using MySQL driver)");
			} else {
				log.debug("Setting databasetype to MARIADB (using MariaDB driver)");
			}
			return new MariaDbDbmsSupport(productVersion);
		} else if (product.equals("H2")) {
			return new H2DbmsSupport(productVersion);
		}
		if (product.startsWith("DB2/")) {
			log.debug("Setting databasetype to DB2 for product [{}]", product);
			return new Db2DbmsSupport();
		}

		for (Dbms dbms : values()) {
			if (dbms.getProductName().equals(product)) {
				log.debug("Setting databasetype to [{}]", dbms);
				IDbmsSupport result;
				try {
					result = dbms.getDbmsSupport();
					log.debug("Returning built-in DBMS [{}] found for product [{}]", dbms, product);
					return result;
				} catch (ReflectiveOperationException | SecurityException e) {
					log.warn("Could not instantiate DbmsSupport for DBMS [{}] found for product [{}]",dbms, product, e);
				}
			}
		}
		log.debug("Returning GenericDbmsSupport for product [{}]", product);
		return new GenericDbmsSupport();
	}

	public IDbmsSupport getDbmsSupport() throws ReflectiveOperationException, SecurityException {
		if (dbmsSupportClass == null) {
			return null;
		}
		return ClassUtils.newInstance(dbmsSupportClass);
	}
}
