/*
 * $Log: GlobalListItem.java,v $
 * Revision 1.7  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2007/10/08 13:35:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.3  2004/06/23 11:31:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configure() removed for aliases
 *
 * Revision 1.2  2004/06/22 11:51:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added aliasFor attribute
 *
 * Revision 1.1  2004/06/21 15:07:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.core.INamedObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Base class for items of global lists.
 * The list itself is contained as a static field.
 * New items are registerd using registerItem().
 * Typical use: SapSystem.getSystem(name).&lt;method to execute&gt;
 * <br/>
 * @version Id
 * @author Gerrit van Brakel
 */
public class GlobalListItem implements INamedObject {
	public static final String version = "$RCSfile: GlobalListItem.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:49 $";
	protected Logger log = LogUtil.getLogger(this);

    private static Hashtable items = new Hashtable();
    private String name;
    private String aliasFor;

	/**
	 * configure() will be called once for each item registered, except for the aliasses.
	 */
	protected void configure() {
	}


    /**
     * Get an item by Name.
     * Descender classes should implement a similar method, that returns an object of its own type.
     */
    protected static GlobalListItem getItem(String itemName) {
		GlobalListItem result = null;

		result = (GlobalListItem)items.get(itemName);
		if (result==null) {
			throw new NullPointerException("no list item found for name ["+itemName+"]");
		}
		if (!StringUtils.isEmpty(result.getAliasFor())) {
			String aliasName = result.getAliasFor();
			result=getItem(aliasName);
			if (result==null) {
				throw new NullPointerException("no alias ["+aliasName+"] list item found for name ["+itemName+"] ");
			}
		}
        return result;
    }
    /**
     * Get the system names as an Iterator, alphabetically sorted
     * @return Iterator with the realm names, alphabetically sorted
     */
    public static Iterator getRegisteredNames() {
        SortedSet sortedKeys = new TreeSet(items.keySet());
        return sortedKeys.iterator();
    }
    /**
     * Get the names as a list
     * @return List with the system names
     */
    public static List getRegisteredNamesAsList() {
        Iterator it = getRegisteredNames();
        List result = new ArrayList();
        while (it.hasNext()) {
            result.add((String) it.next());
        }
        return result;
    }
    
    /**
     * Register an item in the list
     */
    public void registerItem(Object dummyParent) {
    	if (StringUtils.isEmpty(getAliasFor())) {
			configure();
    	}
		items.put(getName(), this);
        log.debug("globalItemList registered item [" + toString() + "]");
    }
    
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * The name under which the item can be retrieved.
	 */
	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}


	/**
	 * If this attribute is set, the item is only an alias for another item.
	 * @param string
	 */
	public void setAliasFor(String string) {
		aliasFor = string;
	}
	public String getAliasFor() {
		return aliasFor;
	}

}
