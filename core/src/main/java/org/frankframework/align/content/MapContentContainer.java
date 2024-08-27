/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.align.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;

import org.frankframework.align.ScalarType;
import org.frankframework.util.LogUtil;

public class MapContentContainer<V> implements DocumentContainer {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private static final String ATTRIBUTE_SEPARATOR = ".";
	private static final String INDEX_SEPARATOR = ".";
	private static final String ARRAY_VALUE_SEPARATOR = ",";
	private final Map<String, List<V>> data;

	private String currentName;
	private String currentValue;
	boolean currentIsNull;
	private ScalarType type=ScalarType.UNKNOWN;

	public MapContentContainer(Map<String,List<V>> data) {
		super();
		this.data=data;
	}

	public static final CharSequenceTranslator ESCAPE_JSON = new AggregateTranslator(new CharSequenceTranslator[] {
			new LookupTranslator(new String[][] { { "\\", "\\\\" } }),
			new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE())});


	public String valueToString(V value) {
		return (String)value;
	}

	public V stringToValue(String value) {
		return (V)value;
	}

	@Override
	public void startElement(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
		currentName=localName;
		currentValue=null;
		currentIsNull=false;
		if (typeDefinition instanceof XSSimpleType simpleType) {
			type=ScalarType.findType(simpleType);
		}
	}

	protected void setValue(String name, V value, boolean isNull) {
		if (value!=null || isNull || type==ScalarType.STRING) {
			List<V> entry=data.get(name);
			if (entry==null) {
				entry=new ArrayList<>();
				data.put(name, entry);
			}
			if (value==null && !isNull && type==ScalarType.STRING) {
				entry.add(stringToValue(""));
			} else {
				entry.add(value);
			}
		}
	}

	@Override
	public void endElement(String localName) {
		setValue(localName,stringToValue(currentValue),currentIsNull);
		currentValue=null;
		currentIsNull=false;
		currentName=null;
		type=null;
	}

	@Override
	public void setNull() {
		currentValue="";
		currentIsNull=true;
	}

	@Override
	public void setAttribute(String name, String value, XSSimpleTypeDefinition attTypeDefinition) {
		setValue(currentName+ATTRIBUTE_SEPARATOR+name,stringToValue(value),false);
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		String rawValue=new String(ch,start,length);
		if (currentName==null) {
			if (rawValue.trim().length()>0) {
				log.warn("no name to set characters [{}]", rawValue);
			}
		} else {
			String value=ESCAPE_JSON.translate(rawValue);
			if (currentValue==null) {
				currentValue=value;
			} else {
				currentValue+=value;
			}
		}
	}

	@Override
	public void startElementGroup(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
//		if (repeatedElement) {
//			Object entry=data.get(localName);
//			if (entry!=null) {
//				if (entry instanceof List) {
//					return;
//				}
//				throw new IllegalStateException("expected list for ["+localName+"] but was ["+entry+"]");
//			}
//			data.put(localName, new ArrayList<String>());
//		}
	}

	@Override
	public void endElementGroup(String localName) {
		// NO OP
	}

	public Map<String,V> flattenedVertical() {
		Map<String,V> result = new LinkedHashMap<>();

		for(String key:data.keySet()) {
			List<V> entry=data.get(key);
			for(int i=0;i<entry.size();i++) {
				result.put(key+INDEX_SEPARATOR+(i+1), entry.get(i));
			}
		}
		return result;
	}

	public Map<String,String> flattenedHorizontal() {
		Map<String,String> result = new LinkedHashMap<>();

		for(String key:data.keySet()) {
			List<V> entry=data.get(key);
			String value="";
			for(int i=0;i<entry.size();i++) {
				if (i>0) value+=ARRAY_VALUE_SEPARATOR;
				value+=valueToString(entry.get(i));
			}
			result.put(key, value);
		}
		return result;
	}

//	public static Map<String,Object> unflattenVertical(Map<String,String> map) {
//		public static Map<String,Object> unflattenHorizontal(Map<String,String> map) {
//			Map<String,Object> result=new LinkedHashMap<String,Object>();
//			for (String key:map.keySet()) {
//				String value=map.get(key);
//				if (dot)
//				if (value.contains(arrayValueSeparator)) {
//					result.put(key, Arrays.asList(value.split(arrayValueSeparator,-1)));
//				} else {
//					result.put(key,value);
//				}
//			}
//		}
//
//	}
	public static Map<String,List<String>> unflatten(Map<String,String> map) {
		Map<String,List<String>> result=new LinkedHashMap<>();
		for (String key:map.keySet()) {
			String value=map.get(key);
			result.put(key, Arrays.asList(value.split(ARRAY_VALUE_SEPARATOR,-1)));
		}
		return result;
	}
}
