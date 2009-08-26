/*
 * $Log: CachedStatGroupTable.java,v $
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
