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
package nl.nn.adapterframework.statistics.jdbc;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.jdbc.JdbcException;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class CachedStatGroupTable extends StatGroupTable {

	private static Map cache=Collections.synchronizedMap(new HashMap());
	private String mapKey;

	public CachedStatGroupTable(String tableName, String keyColumn, String parentKeyColumn, String instanceKeyColumn, String nameColumn, String typeColumn, String sequence) {
		super(tableName,keyColumn,parentKeyColumn,instanceKeyColumn,nameColumn,typeColumn,sequence);
		mapKey=tableName+"/"+keyColumn+"/"+parentKeyColumn+"/"+nameColumn+"/"+typeColumn;
		synchronized(cache) {
			Map tableCache=(Map)cache.get(mapKey);
			if (tableCache==null) {
				tableCache=Collections.synchronizedMap(new HashMap());
				cache.put(mapKey,tableCache);
			}
		}
	}

	public int findOrInsert(Connection connection, int parentKey, int instanceKey, String name, String type) throws JdbcException {
		Integer result;
		Map tableCache=(Map)cache.get(mapKey);
		String valueKey=parentKey+"/"+type+"/"+name;
		result=(Integer)tableCache.get(valueKey);
		if (result==null) {
			result= new Integer(super.findOrInsert(connection, parentKey, instanceKey, name, type));
			tableCache.put(valueKey,result);
		}
		return result.intValue();
	}

}
