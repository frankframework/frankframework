/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.json.stream.JsonGenerator;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Helper class to construct JSON from XML events.
 *
 * @author Gerrit van Brakel
 */
@Log4j2
public class JsonDocumentContainer {

	private static final String ATTRIBUTE_PREFIX = "@";
	private static final String MIXED_CONTENT_LABEL = "#text";

	private static final char[] INDENTOR = "\n                                                                                         ".toCharArray();
	private static final int MAX_INDENT = INDENTOR.length / 2;

	private final Deque<JsonElementContainer> elementStack = new ArrayDeque<>();
	@Getter private final JsonElementContainer root = createElementContainer(null, false, false, null);
	private JsonElementContainer elementContainer=root;

	@Getter private final String name;
	private final boolean skipArrayElementContainers;
	private final boolean skipRootElement;

	public JsonDocumentContainer(String name, boolean skipArrayElementContainers, boolean skipRootElement) {
		this.name = name;
		this.skipArrayElementContainers = skipArrayElementContainers;
		this.skipRootElement = skipRootElement;
	}

	public void startElement(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
		elementStack.push(elementContainer);
		elementContainer = createElementContainer(localName, xmlArrayContainer, repeatedElement, typeDefinition);
	}

	public void endElement() {
		JsonElementContainer result = elementContainer;
		elementContainer = elementStack.pop();
		addContent(elementContainer,result);
	}

	public void setNull() {
		elementContainer.setNull();
	}

	public void setAttribute(String name, String value, XSSimpleTypeDefinition attTypeDefinition) {
		elementContainer.setAttribute(name, value, attTypeDefinition);
	}

	public void characters(char[] ch, int start, int length) {
		elementContainer.characters(ch, start, length);
	}

	protected JsonElementContainer createElementContainer(String localName, boolean xmlArrayContainer, boolean repeatedElement, XSTypeDefinition typeDefinition) {
		return new JsonElementContainer(localName, xmlArrayContainer, repeatedElement, skipArrayElementContainers, ATTRIBUTE_PREFIX, MIXED_CONTENT_LABEL, typeDefinition);
	}

	protected void addContent(JsonElementContainer parent, JsonElementContainer child) {
		if (log.isTraceEnabled())
			log.trace("DocCont.addGroupContent name [{}] child [{}]", parent.getName(), child.getName());
		parent.addContent(child);
	}

	@Override
	public String toString() {
		return toString(true);
	}

	public String toString(boolean indent) {
		Object content = root.getContent();
		if (content == null) {
			return null;
		}
		if (skipRootElement && content instanceof Map<?, ?> map) {
			content = map.values().toArray()[0];
		}
		StringBuilder sb = new StringBuilder();
		toString(sb, content, indent ? 0 : -1);
		return sb.toString();
	}

	protected void toString(StringBuilder sb, Object item, int indentLevel) {
		if (item == null) {
			sb.append("null");
		} else if (item instanceof String) {
			sb.append(item);
		} else if (item instanceof Map) {
			sb.append("{");
			if (indentLevel >= 0) indentLevel++;
			//noinspection unchecked
			for (Entry<String, Object> entry : ((Map<String, Object>) item).entrySet()) {
				newLine(sb, indentLevel);
				sb.append('"').append(entry.getKey()).append("\": ");
				toString(sb, entry.getValue(), indentLevel);
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			if (indentLevel >= 0) indentLevel--;
			newLine(sb, indentLevel);
			sb.append("}");
		} else if (item instanceof List<?> list) {
			sb.append("[");
			if (indentLevel >= 0) indentLevel++;
			for (Object subitem : list) {
				newLine(sb, indentLevel);
				toString(sb, subitem, indentLevel);
				sb.append(",");
			}
			sb.deleteCharAt(sb.length() - 1);
			if (indentLevel >= 0) indentLevel--;
			newLine(sb, indentLevel);
			sb.append("]");
		} else if (item instanceof JsonElementContainer container) {
			toString(sb, container.getContent(), indentLevel);
		} else {
			throw new NotImplementedException("cannot handle class [" + item.getClass().getName() + "]");
		}
	}

	protected void generate(JsonGenerator g, String key, Object item) {
		if (item == null) {
			if (key != null) g.writeNull(key);
			else g.writeNull();
		} else if (item instanceof String string) {
			if (key != null) g.write(key, string);
			else g.write(string);
		} else if (item instanceof Map) {
			if (key != null) g.writeStartObject(key);
			else g.writeStartObject();
			//noinspection unchecked
			for (Entry<String, Object> entry : ((Map<String, Object>) item).entrySet()) {
				generate(g, entry.getKey(), entry.getValue());
			}
			g.writeEnd();
		} else if (item instanceof List<?> list) {
			if (key != null) g.writeStartArray(key);
			else g.writeStartArray();
			for (Object subitem : list) {
				generate(g, null, subitem);
			}
			g.writeEnd();
		} else {
			throw new NotImplementedException("cannot handle class [" + item.getClass().getName() + "]");
		}
	}

	private void newLine(StringBuilder sb, int indentLevel) {
		if (indentLevel >= 0) {
			sb.append(INDENTOR, 0, (Math.min(indentLevel, MAX_INDENT)) * 2 + 1);
		}
	}
}
