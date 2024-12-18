/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2024 WeAreFrank!

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.align.ScalarType;

/**
 * Helper class to construct JSON from XML events.
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class JsonElementContainer {
	public static final CharSequenceTranslator ESCAPE_JSON = new AggregateTranslator(new CharSequenceTranslator[] {
			new LookupTranslator(new String[][] { { "\"", "\\\"" }, { "\\", "\\\\" } }),
			new LookupTranslator(EntityArrays.JAVA_CTRL_CHARS_ESCAPE())
	});

	@Getter private final String name;
	@Getter private final boolean xmlArrayContainer;
	@Getter private final boolean repeatedElement;
	private final boolean skipArrayElementContainers;
	private boolean nil=false;
	@Setter @Getter private ScalarType type=ScalarType.UNKNOWN;
	private final String attributePrefix;
	private final String mixedContentLabel;

	private String stringContent;
	private Map<String,Object> contentMap;
	private List<Object> array;

	public JsonElementContainer(String name, boolean xmlArrayContainer, boolean repeatedElement, boolean skipArrayElementContainers, String attributePrefix, String mixedContentLabel, XSTypeDefinition typeDefinition) {
		this.name = name;
		this.xmlArrayContainer = xmlArrayContainer;
		this.repeatedElement = repeatedElement;
		this.skipArrayElementContainers = skipArrayElementContainers;
		this.attributePrefix = attributePrefix;
		this.mixedContentLabel = mixedContentLabel;
		if (typeDefinition != null) {
			switch(typeDefinition.getTypeCategory()) {
			case XSTypeDefinition.SIMPLE_TYPE:
				setType(ScalarType.findType(((XSSimpleType)typeDefinition)));
				break;
			case XSTypeDefinition.COMPLEX_TYPE:
				XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
				switch (complexTypeDefinition.getContentType()) {
				case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
					if (log.isTraceEnabled()) log.trace("JsonElementContainer complexTypeDefinition.contentType is Empty, no child elements");
					break;
				case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
					if (log.isTraceEnabled()) log.trace("JsonElementContainer complexTypeDefinition.contentType is Simple, no child elements (only characters)");
					setType(ScalarType.findType((XSSimpleType)complexTypeDefinition.getBaseType()));
					break;
				case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
					if (log.isTraceEnabled()) log.trace("JsonElementContainer complexTypeDefinition.contentType is Element");
					break;
				case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
					if (log.isTraceEnabled()) log.trace("JsonElementContainer complexTypeDefinition.contentType is Mixed");
					break;
				}
			}
		}
	}

	public void setNull() {
		setContent(null);
	}

	public void setAttribute(String name, String value, XSSimpleTypeDefinition attTypeDefinition) {
		JsonElementContainer attributeContainer = new JsonElementContainer(attributePrefix+name, false, false, false, attributePrefix, mixedContentLabel, attTypeDefinition);
		attributeContainer.setContent(value);
		addContent(attributeContainer);
	}

	public void characters(char[] ch, int start, int length) {
		setContent(new String(ch,start,length));
	}

	/*
	 * Sets the Text content of the current object
	 */
	public void setContent(String content) {
		if (log.isTraceEnabled()) log.trace("setContent() name [{}] content [{}]", getName(), content);
		if (content != null) {
			boolean whitespace = content.trim().isEmpty();
			if (whitespace && stringContent == null) {
				if (log.isTraceEnabled()) log.trace("setContent() ignoring empty content for name [{}]", getName());
				return;
			}
		}
		if (contentMap != null) {
			if (StringUtils.isNotEmpty(mixedContentLabel)) {
				JsonElementContainer textContainer = (JsonElementContainer) contentMap.get(mixedContentLabel);
				if (textContainer == null) {
					textContainer = new JsonElementContainer(mixedContentLabel, false, false, false, attributePrefix, mixedContentLabel, null);
					textContainer.setType(getType());
					contentMap.put(mixedContentLabel, textContainer);
					textContainer.stringContent = content;
				} else {
					textContainer.stringContent += content;
				}
				return;
			}
			throw new IllegalStateException("already created map for element [" + name + "] and no mixexContentLabel set");
		}
		if (array != null) {
			throw new IllegalStateException("already created array for element [" + name + "]");
		}
		if (nil) {
			throw new IllegalStateException("already set nil for element [" + name + "]");
		}
		if (content == null) {
			if (stringContent != null) {
				throw new IllegalStateException("already set non-null content for element [" + name + "]");
			}
			nil = true;
		} else {
			if (stringContent == null) {
				stringContent = content;
			} else {
				stringContent += content;
			}
			log.trace("resulting stringContent [{}] toString [{}]", () -> stringContent, this::toString);
		}
	}

	/*
	 * connects child to parent
	 */
	public void addContent(JsonElementContainer content) {
		String childName = content.getName();
		if (log.isTraceEnabled()) {
			log.trace("addContent for parent [{}] name [{}] array container [{}] content.isRepeatedElement [{}] skipArrayElementContainers [{}] content [{}]", getName(), childName, isXmlArrayContainer(), content.isRepeatedElement(), skipArrayElementContainers, content);
		}

		if (stringContent != null) {
			String error = String.format("Could not add child element '%s' to element '%s' because it already has text content (%s). You might have an " +
							"unrecognized element in your input which was added as text content", childName, this.name, stringContent.trim());

			throw new IllegalStateException(error);
		}

		if (isXmlArrayContainer() && content.isRepeatedElement() && skipArrayElementContainers) {
			if (array == null) {
				array = new ArrayList<>();
				setType(content.getType());
			}
			array.add(content.getContent());
			return;
		}
		if (array != null) {
			throw new IllegalStateException("already created array for element [" + name + "]");
		}
		if (contentMap == null) {
			contentMap = new LinkedHashMap<>();
		}
		Object current = contentMap.get(childName);
		if (content.isRepeatedElement()) {
			if (current == null) {
				current = new ArrayList<>();
				contentMap.put(childName, current);
			} else {
				if (!(current instanceof List)) {
					throw new IllegalArgumentException("element [" + childName + "] is not an array");
				}
			}
			// noinspection unchecked,rawtypes
			((List) current).add(content.getContent());
		} else {
			if (current != null) {
				throw new IllegalStateException("element [" + childName + "] content already set to [" + current + "]");
			}
			contentMap.put(childName, content.getContent());
		}
	}

	public static String stripLeadingZeroes(String value) {
		if (value.length() > 1) {    // check for leading zeroes, and remove them.
			boolean negative = value.charAt(0) == '-';
			int i = negative ? 1 : 0;
			while (i < value.length() - 1 && value.charAt(i) == '0' && Character.isDigit(value.charAt(i + 1))) {
				i++;
			}
			if (i > (negative ? 1 : 0)) {
				return (negative ? "-" : "") + value.substring(i);
			}
		}
		return value;
	}

	public Object getContent() {
		if (nil) {
			return null;
		}
		if(stringContent != null) {
			switch (getType()) {
			case BOOLEAN:
				return stringContent;
			case NUMERIC:
				return stripLeadingZeroes(stringContent);
			default:
				if(log.isTraceEnabled()) log.trace("getContent quoted stringContent [{}]", stringContent);
				String result = ESCAPE_JSON.translate(stringContent);
				return '"' + result + '"';
			}
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
		if (getType()==ScalarType.STRING) {
			return "\"\"";
		}
		return "{}";
	}

	@Override
	public String toString() {
		Object content = getContent();
		return content == null ? "<null>" : content.toString();
	}
}
