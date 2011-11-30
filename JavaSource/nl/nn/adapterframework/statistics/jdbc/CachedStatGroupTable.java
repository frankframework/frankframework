/*
 * $Log: CachedStatGroupTable.java,v $
 * Revision 1.3  2011-11-30 13:52:03  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/01/07 13:16:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics related classes to statistics package
 *
 * Revision 1.1  2009/08/26 15:35:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for storing statistics in a database
 *
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
