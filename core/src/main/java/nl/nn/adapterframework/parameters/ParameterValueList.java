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
package nl.nn.adapterframework.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.stream.Message;

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
	
	public static ParameterValueList get(ParameterList params, Message message, PipeLineSession session) throws ParameterException {
		if (params==null) {
			return null;
		}
		return params.getValues(message, session);
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

	public Object getValue(int i) {
		return list.get(i).getValue();
	}

	public String getName(int i) {
		return list.get(i).getName();
	}

	public Object getValue(String name) {
		ParameterValue pv = map.get(name);
		return pv ==null ? null : pv.getValue();
	}

	public boolean containsKey(String name) {
		return map.containsKey(name);
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

	/**
	 * Returns a Map of value objects
	 */
	public Map<String,Object> getValueMap() throws ParameterException {
		Map<String, ParameterValue> paramValuesMap = getParameterValueMap();

		// convert map with parameterValue to map with value		
		Map<String,Object> result = new LinkedHashMap<String,Object>(paramValuesMap.size());
		for (ParameterValue pv : paramValuesMap.values()) {
			result.put(pv.getDefinition().getName(), pv.getValue());
		}
		return result;
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
