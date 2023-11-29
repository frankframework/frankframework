/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.dbms.JdbcException;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author  Gerrit van Brakel
 * @since
 */
public class CachedSideTable extends SideTable {

	private static final Map cache = Collections.synchronizedMap(new HashMap());
	private final String mapKey;

	public CachedSideTable(String tableName, String keyColumn, String nameColumn, String sequence) {
		super(tableName, keyColumn, nameColumn, sequence);
		mapKey=tableName+"/"+keyColumn+"/"+nameColumn;
		synchronized(cache) {
			Map tableCache=(Map)cache.get(mapKey);
			if (tableCache==null) {
				tableCache=Collections.synchronizedMap(new HashMap());
				cache.put(mapKey,tableCache);
			}
		}
	}

	public int findOrInsert(Connection connection, String name) throws JdbcException {
		Integer result;
		Map tableCache=(Map)cache.get(mapKey);
		result=(Integer)tableCache.get(name);
		if (result==null) {
			result= new Integer(super.findOrInsert(connection, name));
			tableCache.put(name,result);
		}
		return result.intValue();
	}

}
