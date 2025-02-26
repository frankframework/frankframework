/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.documentbuilder.json;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import org.apache.commons.text.StringEscapeUtils;
import org.xml.sax.SAXException;

import org.frankframework.documentbuilder.JsonEventHandler;
import org.frankframework.xml.SaxException;

public class JsonWriter implements JsonEventHandler {

	private final Writer writer;
	private boolean closeWriterOnEndDocument = false;

	private final Deque<NodeState> stateStack = new ArrayDeque<>();

	private static class NodeState {
		private boolean firstElemSeen;
		private final boolean inArray;

		private NodeState(boolean inArray) {
			this.inArray = inArray;
		}
	}

	/** When you supply a {@link Writer} you will have to close it. */
	public JsonWriter(Writer writer) {
		this(writer, false);
	}

	public JsonWriter(Writer writer, boolean closeWriterOnEndDocument) {
		this.writer = writer;
		stateStack.push(new NodeState(false));
		this.closeWriterOnEndDocument = closeWriterOnEndDocument;
	}

	@Override
	public void startDocument() {
		// nothing special
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			if (closeWriterOnEndDocument) {
				writer.close();
			} else {
				writer.flush();
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	private void writeSeparatingComma(boolean beforeKey) throws IOException {
		NodeState state = Objects.requireNonNull(stateStack.peek(), "StateStack should not be empty when this method is called");
		if (beforeKey || state.inArray) {
			if (state.firstElemSeen) {
				writer.write(",");
			} else {
				state.firstElemSeen=true;
			}
		}
	}

	@Override
	public void startObject() throws SAXException {
		try {
			writeSeparatingComma(false);
			stateStack.push(new NodeState(false));
			writer.write("{");
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startObjectEntry(String key) throws SAXException {
		try {
			writeSeparatingComma(true);
			writer.write("\""+key+"\":");
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endObject() throws SAXException {
		try {
			stateStack.pop();
			writer.write("}");
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void startArray() throws SAXException {
		try {
			writeSeparatingComma(false);
			stateStack.push(new NodeState(true));
			writer.write("[");
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void endArray() throws SAXException {
		try {
			stateStack.pop();
			writer.write("]");
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void primitive(Object value) throws SaxException {
		try {
			writeSeparatingComma(false);
			if (value instanceof String string) {
				writer.write("\""+StringEscapeUtils.escapeJson(string)+"\"");
			} else if (value == null) {
				writer.write("null");
			} else {
				writer.write(StringEscapeUtils.escapeJson(value.toString()));
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public void number(String value) throws SAXException {
		try {
			writeSeparatingComma(false);
			writer.write(Objects.requireNonNullElse(value, "null"));
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	@Override
	public String toString() {
		return writer.toString();
	}
}
