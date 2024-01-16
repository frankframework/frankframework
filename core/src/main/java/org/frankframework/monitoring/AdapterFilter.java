/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.monitoring;

import java.util.ArrayList;
import java.util.List;

import org.frankframework.doc.FrankDocGroup;

/**
 * Filter on Adapters, used by Triggers.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.8
 */
@FrankDocGroup(name = "Monitoring")
public class AdapterFilter {

	private String adapter;

	private List<String> subObjectList=new ArrayList<>();

	/**
	 * Set the name of the Adapter that this AdapterFilter filters on.
	 */
	public void setAdapter(String string) {
		adapter = string;
	}
	public String getAdapter() {
		return adapter;
	}

	public boolean isFilteringToLowerLevelObjects() {
		return !subObjectList.isEmpty();
	}

	/**
	 * Register the name of a SubObject (such as a Pipe) to be included in the filter.
	 */
	public void registerSubObject(String name) {
		subObjectList.add(name);
	}
	/**
	 * Get the list of registered names of SubObjects included in the filter.
	 */
	public List<String> getSubObjectList() {
		return subObjectList;
	}
}
