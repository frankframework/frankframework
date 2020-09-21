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
import org.apache.logging.log4j.Logger;

/**
 * Base class for items of global lists.
 * The list itself is contained as a static field.
 * New items are registerd using registerItem().
 * Typical use: SapSystem.getSystem(name).&lt;method to execute&gt;
 * <br/>
 * @author Gerrit van Brakel
 */
public class GlobalListItem implements INamedObject {
	protected Logger log = LogUtil.getLogger(this);

    private static Hashtable<String, GlobalListItem> items = new Hashtable<String, GlobalListItem>();
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

		result = items.get(itemName);
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
    public static Iterator<String> getRegisteredNames() {
        SortedSet<String> sortedKeys = new TreeSet<String>(items.keySet());
        return sortedKeys.iterator();
    }
    /**
     * Gets a list with system names.
     */
    public static List<String> getRegisteredNamesAsList() {
        Iterator<String> it = getRegisteredNames();
        List<String> result = new ArrayList<String>();
        while (it.hasNext()) {
            result.add(it.next());
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
	 */
	public void setAliasFor(String string) {
		aliasFor = string;
	}
	public String getAliasFor() {
		return aliasFor;
	}

}
