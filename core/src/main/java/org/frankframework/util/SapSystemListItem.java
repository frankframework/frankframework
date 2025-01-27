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
package org.frankframework.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.HasName;
import org.frankframework.core.NameAware;

/**
 * Base class for items of global lists.
 * The list itself is contained as a static field.
 * New items are registerd using registerItem().
 * Typical use: SapSystem.getSystem(name).&lt;method to execute&gt;
 * <br/>
 * @author Gerrit van Brakel
 */
public abstract class SapSystemListItem implements NameAware, HasName {
	protected Logger log = LogUtil.getLogger(this);

	private static final Hashtable<String, SapSystemListItem> items = new Hashtable<>();
	private String name;
	private String aliasFor;

	/**
	 * configure() will be called once for each item registered, except for the aliasses.
	 */
	public void configure() {
	}

	/**
	 * Get an item by Name. Descender classes should implement a similar method,
	 * that returns an object of its own type.
	 */
	protected static SapSystemListItem getItem(String itemName) {
		SapSystemListItem result = null;

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
	 *
	 * @return Iterator with the realm names, alphabetically sorted
	 */
	public static Iterator<String> getRegisteredNames() {
		SortedSet<String> sortedKeys = new TreeSet<>(items.keySet());
		return sortedKeys.iterator();
	}

	/**
	 * Gets a list with system names.
	 */
	public static List<String> getRegisteredNamesAsList() {
		Iterator<String> it = getRegisteredNames();
		List<String> result = new ArrayList<>();
		while(it.hasNext()) {
			result.add(it.next());
		}
		return result;
	}

	/**
	 * Register an item in the list
	 */
	public static void registerItem(SapSystemListItem item) {
		if(StringUtils.isEmpty(item.getAliasFor())) {
			item.configure();
		}
		items.put(item.getName(), item);
	}

	public static void clear() {
		items.clear();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	/**
	 * The name under which the item can be retrieved.
	 */
	@Override
	public void setName(String string) {
		name = string;
	}
	@Override
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
