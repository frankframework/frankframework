/*
 * $Log: CachedSideTable.java,v $
 * Revision 1.3  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/08/26 15:35:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
