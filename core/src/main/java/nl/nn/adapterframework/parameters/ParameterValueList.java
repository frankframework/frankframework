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
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.ParameterException;

/**
 * List of parametervalues.
 * 
 * @author Gerrit van Brakel
 */
public class ParameterValueList {
	
	List<ParameterValue> list;
	Map<String, ParameterValue> map;
	
	public ParameterValueList() {
		super();
		list = new ArrayList<ParameterValue>();
		map  = new HashMap<String, ParameterValue>();
	}

	public ParameterValueList(int i) {
		super();
		list = new ArrayList<ParameterValue>(i);
		map  = new HashMap<String, ParameterValue>();
	}
	
	public void add(ParameterValue pv) {
		list.add(pv);
		map.put(pv.getDefinition().getName(),pv);
	}
	
	public ParameterValue getParameterValue(int i) {
		return list.get(i);
	}

	public ParameterValue getParameterValue(String name) {
		return map.get(name);
	}

	public ParameterValue removeParameterValue(String name) {
		ParameterValue pv = map.remove(name);
		if (pv!=null) {
			list.remove(pv);
		}
		return pv;
	}
	
	public boolean parameterExists(String name) {
		return map.get(name)!=null;
	}

	public int size() {
		return list.size();
	}
	
	Map<String, ParameterValue> getParameterValueMap() {
		return map;
	}

	/*
	 * Helper routine for quickly iterating through the resolved parameters
	 * in the order in which they are defined 
	 */
	public void forAllParameters(IParameterHandler handler) throws ParameterException {
		for (Iterator<ParameterValue> param = list.iterator(); param.hasNext();) {
			ParameterValue paramValue = param.next();
			handler.handleParam(paramValue.getDefinition().getName(), paramValue.getValue());
		}
	}
}
