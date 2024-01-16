/*
   Copyright 2022, 2023 WeAreFrank!

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
package org.frankframework.stream.document;

import java.util.Stack;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.stream.JsonEventHandler;
import org.frankframework.xml.SaxException;

public class Json2XmlHandler implements JsonEventHandler {

	public static String DEFAULT_ARRAY_ELEMENT_NAME = "item";

	private String root;
	private ContentHandler handler;
	private boolean prettyPrint;

	private Stack<Object> stack = new Stack<>();


	public Json2XmlHandler(ContentHandler handler, boolean prettyPrint) {
		this("root", handler, prettyPrint);
	}

	public Json2XmlHandler(String root, ContentHandler handler, boolean prettyPrint) {
		this.root = root;
		this.handler = handler;
		this.prettyPrint = prettyPrint;
	}

	@Override
	public void startDocument() throws SAXException {
		stack.push(new XmlDocumentBuilder(root, handler, prettyPrint));
	}

	@Override
	public void endDocument() throws SAXException {
		if (!stack.isEmpty()) {
			((XmlDocumentBuilder)stack.pop()).close();
		}
	}

	private void checkPendingFieldOrElement() throws SAXException {
		Object top=stack.peek();
		if (top instanceof INodeBuilder) {
			((INodeBuilder)stack.pop()).close();
		}
	}

	private Object checkField() throws SAXException {
		Object top = stack.peek();
		if (top instanceof String) {
			String key = (String)stack.pop();
			ObjectBuilder objectBuilder = (ObjectBuilder)stack.peek();
			top=objectBuilder.addField(key);
			stack.push(top);
			return top;
		}
		if (top instanceof ArrayBuilder) {
			top=((ArrayBuilder)top).addElement();
			stack.push(top);
			return top;
		}
		return top;
	}

	@Override
	public void startObject() throws SAXException {
		Object top = checkField();
		if (top instanceof IDocumentBuilder) {
			stack.push(((IDocumentBuilder)top).asObjectBuilder());
			return;
		}
		if (top instanceof INodeBuilder) {
			stack.push(((INodeBuilder)top).startObject());
			return;
		}
		throw new SaxException("Do not expect startObject() with stack top ["+top+"]");
	}

	@Override
	public void startObjectEntry(String key) throws SAXException {
		Object top = stack.peek();
		if (top instanceof ObjectBuilder) {
			stack.push(key);
			return;
		}
		throw new SaxException("Do not expect startObjectEntry() with stack top ["+top+"]");
	}

	@Override
	public void endObject() throws SAXException {
		((ObjectBuilder)stack.pop()).close();
		checkPendingFieldOrElement();
	}

	@Override
	public void startArray() throws SAXException {
		Object top = stack.peek();
		if (top instanceof IDocumentBuilder) {
			stack.push(((IDocumentBuilder)top).asArrayBuilder(DEFAULT_ARRAY_ELEMENT_NAME));
			return;
		}
		if (top instanceof ArrayBuilder) {
			stack.push(((ArrayBuilder)top).addArrayElement(DEFAULT_ARRAY_ELEMENT_NAME));
			return;
		}
		if (top instanceof String) {
			String key = (String)stack.pop();
			ObjectBuilder objectBuilder = (ObjectBuilder)stack.peek();
			top=objectBuilder.addRepeatedField(key);
			stack.push(top);
			return;
		}
		throw new SaxException("Do not expect startObject() with stack top ["+top+"]");
	}

	@Override
	public void endArray() throws SAXException {
		((ArrayBuilder)stack.pop()).close();
		checkPendingFieldOrElement();
	}

	@Override
	public void primitive(Object value) throws SAXException {
		INodeBuilder top = (INodeBuilder)checkField();
		try (INodeBuilder node = top) {
			if (value instanceof String) {
				top.setValue((String)value);
			} else if (value instanceof Boolean) {
				top.setValue((Boolean)value);
			} else if (value instanceof Number) {
				top.setValue((Number)value);
			} else if (value==null) {
				top.setValue((String)null);
			} else {
				top.setValue(value.toString());
			}
		}
		stack.pop();
	}

	@Override
	public void number(String value) throws SAXException {
		INodeBuilder top = (INodeBuilder)checkField();
		try (INodeBuilder node = top) {
			top.setNumberValue(value);
		}
		stack.pop();
	}


}
