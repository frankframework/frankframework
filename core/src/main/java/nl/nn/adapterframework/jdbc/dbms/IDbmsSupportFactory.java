/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import javax.sql.DataSource;

/**
 * @author  Gerrit van Brakel
 */
public interface IDbmsSupportFactory {

	final int DBMS_GENERIC=0;
	final int DBMS_ORACLE=1;
	final int DBMS_MSSQLSERVER=2;
	final int DBMS_DB2=3;
	final int DBMS_H2=4;
	final int DBMS_MYSQL=5;

	IDbmsSupport getDbmsSupport(DataSource datasource);

}
