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

public enum Dbms {

	NONE("none"),
	GENERIC("generic"),
	ORACLE("Oracle"),
	MSSQL("MS_SQL", "Microsoft SQL Server"),
	DB2("DB2"),
	H2("H2"),
	MYSQL("MySQL"),
	MARIADB("MariaDB");
	
	private String key;
	private String productName;
	
	private Dbms(String key) {
		this(key, key);
	}
	private Dbms(String key, String productName) {
		this.key = key;
		this.productName = productName;
	}

	public String getKey() {
		return key;
	}

	public String getProductName() {
		return productName;
	}
}
