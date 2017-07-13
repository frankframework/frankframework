/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class to construct JSON from XML events.
 * 
 * @author Gerrit van Brakel
 */
public class JsonContentContainer {
	protected Logger log = Logger.getLogger(this.getClass());
	
	private String name;
	private boolean arrayElement;
	public StringBuffer stringContent;
	private Map<String,Object> contentMap;
	private JSONArray array;
	
	private boolean skipArrayElementContainers;
	
	private final boolean DEBUG=false; 	
	
	public JsonContentContainer(String name, boolean arrayElement, boolean skipArrayElementContainers) {
		this.name=name;
		this.arrayElement=arrayElement;
		this.skipArrayElementContainers=skipArrayElementContainers;
	}
	

	public String getName() {
		return name;
	}
	public boolean isArrayElement() {
		return arrayElement;
	}

	public void setContent(String content) {
		if (DEBUG && log.isDebugEnabled()) log.debug("setContent name ["+getName()+"] content ["+content+"]");
		content=content.trim();
		if (content.isEmpty()) {
			if (DEBUG && log.isDebugEnabled()) log.debug("setContent ignoring empty content for name ["+getName()+"]");
			return;
		}
		if (contentMap!=null) {
			throw new IllegalStateException("already created map for element ["+name+"]");
		}
		if (array!=null) {
			throw new IllegalStateException("already created array for element ["+name+"]");
		}
		if (stringContent==null) {
			stringContent=new StringBuffer(content);
		} else {
			stringContent.append(content);
		}
	}
	public void addContent(JsonContentContainer content) {
		String childName=content.getName();
		if (DEBUG && log.isDebugEnabled()) log.debug("addContent for parent ["+getName()+"] name ["+childName+"] content ["+content+"]");
		if (stringContent!=null) {
			throw new IllegalStateException("content already set as String for element ["+getName()+"]");
		}
		if (content.isArrayElement() && skipArrayElementContainers) {
			if (array==null) {
				array=new JSONArray();
			} 
			array.put(content.getContent());
			return;
		}
		if (array!=null) {
			throw new IllegalStateException("already created array for element ["+name+"]");
		}
		if (contentMap==null) {
			contentMap=new LinkedHashMap<String,Object>();
		}
		Object current=contentMap.get(childName);
		if (content.isArrayElement()) {
			if (current==null) {
				current=new JSONArray();
				contentMap.put(childName,current);
			} else {
				if (!(current instanceof JSONArray)) {
					throw new IllegalArgumentException("element ["+childName+"] is not an array");
				}
			}
			((JSONArray)current).put(content.getContent());
		} else {
			if (current!=null) {
				throw new IllegalStateException("content already set for element ["+childName+"]");
			}
			contentMap.put(childName, content.getContent());
		}
	}

	public Object getContent() {
		if (stringContent!=null) {
			return stringContent;
		}
		if (array!=null) {
			return array;
		}
		if (contentMap!=null) {
			return new JSONObject(contentMap);
		}
		return new JSONObject();
	}

	public JSONObject toJson() {
		Object content=getContent();
		if (content==null) {
			return new JSONObject();
		}
		if (content instanceof JSONObject) {
			return (JSONObject)content;
		}
		return new JSONObject(content);
	}
	
	@Override
	public String toString() {
		Object content=getContent();
		if (content==null) {
			return null;
		}
		if (content instanceof JSONObject) {
			try {
				String result=((JSONObject)content).toString(2);
				return result.replaceAll("\\\\u20ac", "€"); // TODO: Do something structural for diacritics! This is a hack for cosmetics!
			} catch (JSONException e) {
				log.warn(e);
			}
		}
		return content.toString();
	}
}
