/*
 * $Log: GlobalListItem.java,v $
 * Revision 1.1  2004-06-21 15:07:51  L190409
 * first version
 *
 *
 */
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.core.INamedObject;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;

/**
 * Base class for items of global lists.
 * The list itself is contained as a static field.
 * New items are registerd using registerItem().
 * Typical use: SapSystemFactory.getInstance().&lt;method to execute&gt;
 * <br/>
 * @version Id
 * @author Gerrit van Brakel
 */
public class GlobalListItem implements INamedObject {
	public static final String version="$Id: GlobalListItem.java,v 1.1 2004-06-21 15:07:51 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());;

    private static Hashtable items = new Hashtable();
    private String name;

	protected void configure() {
	}


    /**
     * Get an item by Name.
     * Descender classes should implement a similar method, that returns an object of its own type.
     */
    protected static Object getItem(String itemName) {
        Object result = null;

		result = items.get(itemName);
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
     * @return ArrayList with the system names
     */
    public static ArrayList getRegisteredNamesAsList() {
        Iterator it = getRegisteredNames();
        ArrayList result = new ArrayList();
        while (it.hasNext()) {
            result.add((String) it.next());
        }
        return result;
    }
    /**
     * register an item
     * @param sapSystem
     */
    public void registerItem(Object dummyParent) {
		this.configure();
		items.put(getName(), this);
        log.debug("globalItemList registered item [" + toString() + "]");
    }
    
    public String toString(){
        return ToStringBuilder.reflectionToString(this);
    }
	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param string
	 */
	public void setName(String string) {
		name = string;
	}

}
