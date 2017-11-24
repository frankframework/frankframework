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
package nl.nn.adapterframework.align.content;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

/**
 * Helper class to construct JSON from XML events.
 * 
 * @author Gerrit van Brakel
 */
public class JsonElementContainer implements ElementContainer {
	protected Logger log = Logger.getLogger(this.getClass());
	
	private String name;
	private boolean xmlArrayContainer;
	private boolean repeatedElement;
	private boolean skipArrayElementContainers;
	private boolean nil=false;
	private boolean quoted=true;
	private String attributePrefix;

	public String stringContent;
	private Map<String,Object> contentMap;
	private List<Object> array;
	
	private final boolean DEBUG=false; 	
	
	public JsonElementContainer(String name, boolean xmlArrayContainer, boolean repeatedElement, boolean skipArrayElementContainers, String attributePrefix) {
		this.name=name;
		this.xmlArrayContainer=xmlArrayContainer;
		this.repeatedElement=repeatedElement;
		this.skipArrayElementContainers=skipArrayElementContainers;
		this.attributePrefix=attributePrefix;
	}
	
	public static final CharSequenceTranslator ESCAPE_JSON = new AggregateTranslator(new CharSequenceTranslator[] {
			new LookupTranslator(new String[][] { { "\"", "\\\"" }, { "\\", "\\\\" }, { "/", "\\/" } }),
			new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE())});

	@Override
	public void setNull() {
		setContent(null);
	}

	@Override
	public void setAttribute(String name, String value, XSSimpleTypeDefinition attTypeDefinition) {
		JsonElementContainer attributeContainer = new JsonElementContainer(attributePrefix+name, false, false, false, attributePrefix);
		if (attTypeDefinition.getNumeric()) {
			attributeContainer.setQuoted(false);
		}
		attributeContainer.setContent(value);
		addContent(attributeContainer);
	}

	@Override
	public void characters(char[] ch, int start, int length, boolean numericType, boolean booleanType) {
		if (numericType || booleanType) {
			setQuoted(false);
		}
		setContent(new String(ch,start,length));
	}

	
	/*
	 * Sets the Text content of the current object
	 */
	public void setContent(String content) {
		if (DEBUG) log.debug("setContent name ["+getName()+"] content ["+content+"]");
		if (content!=null) {
			boolean whitespace=content.trim().isEmpty();
			if (whitespace && stringContent==null) {
				if (DEBUG) log.debug("setContent ignoring empty content for name ["+getName()+"]");
				return;
			}
		}
		if (contentMap!=null) {
			throw new IllegalStateException("already created map for element ["+name+"]");
		}
		if (array!=null) {
			throw new IllegalStateException("already created array for element ["+name+"]");
		}
		if (nil) {
			throw new IllegalStateException("already set nil for element ["+name+"]");
		}
		if (content==null) {
			if (stringContent!=null) {
				throw new IllegalStateException("already set non-null content for element ["+name+"]");
			}
			nil=true;
			stringContent="null";
			quoted=false;
		} else {
			if (stringContent==null) {
				stringContent=content;
			} else {
				stringContent+=content;
			}
			if (DEBUG) log.debug("resulting stringContent ["+stringContent+"] stringContent.toString ["+stringContent.toString()+"] toString ["+toString()+"]");
		}
	}
	
	/*
	 * connects child to parent
	 */
	public void addContent(JsonElementContainer content) {
		String childName=content.getName();
		if (DEBUG) log.debug("addContent for parent ["+getName()+"] name ["+childName+"] array container ["+isXmlArrayContainer()+"] content.isRepeatedElement ["+content.isRepeatedElement()+"] skipArrayElementContainers ["+skipArrayElementContainers+"] content ["+content+"]");
		if (stringContent!=null) {
			throw new IllegalStateException("content already set as String for element ["+getName()+"]");
		}
		if (isXmlArrayContainer() && content.isRepeatedElement() && skipArrayElementContainers) {
			if (array==null) {
				array=new LinkedList<Object>();
				setQuoted(content.isQuoted());
			} 
			array.add(content.getContent());
			return;
		}
		if (array!=null) {
			throw new IllegalStateException("already created array for element ["+name+"]");
		}
		if (contentMap==null) {
			contentMap=new LinkedHashMap<String,Object>();
		}
		Object current=contentMap.get(childName);
		if (content.isRepeatedElement()) {
			if (current==null) {
				current=new LinkedList<Object>();
				contentMap.put(childName,current);
			} else {
				if (!(current instanceof List)) {
					throw new IllegalArgumentException("element ["+childName+"] is not an array");
				}
			}
			((List)current).add(content.getContent());
		} else {
			if (current!=null) {
				throw new IllegalStateException("element ["+childName+"] content already set to ["+current+"]");
			}
			contentMap.put(childName, content.getContent());
		}
	}

	public Object getContent() {
		if (nil) {
			return null;
		}
		if (stringContent!=null) {
			if (quoted) {
				if (DEBUG) log.debug("getContent quoted stringContent ["+stringContent+"]");
//				String result=StringEscapeUtils.escapeJson(stringContent.toString()); // this also converts diacritics into unicode escape sequences
				String result=ESCAPE_JSON.translate(stringContent.toString()); 
				return '"'+result+'"';
			}
			return stringContent;
		}
		if (array!=null) {
			return array;
		}
		if (contentMap!=null) {
			return contentMap;
		}
		if (isXmlArrayContainer() && skipArrayElementContainers) {
			return "[]";
		}
		return "{}";
	}


	public String getName() {
		return name;
	}
	public boolean isXmlArrayContainer() {
		return xmlArrayContainer;
	}
	public boolean isRepeatedElement() {
		return repeatedElement;
	}

	public boolean isQuoted() {
		return quoted;
	}
	public void setQuoted(boolean quoted) {
		this.quoted = quoted;
	}

	
}
