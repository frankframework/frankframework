/*
 * $Log: CachedSideTable.java,v $
 * Revision 1.1  2009-08-26 15:35:11  L190409
 * support for storing statistics in a database
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class CachedSideTable extends SideTable {
	
	private static Map cache=Collections.synchronizedMap(new HashMap());
	private String mapKey;
	
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
