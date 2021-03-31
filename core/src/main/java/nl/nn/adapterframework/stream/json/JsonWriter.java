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
package nl.nn.adapterframework.stream.json;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Stack;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.JsonEventHandler;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.xml.SaxException;

public class JsonWriter implements JsonEventHandler {

	private Writer writer;
	
	private Stack<NodeState> stateStack = new Stack<>();
	
	private class NodeState {
		private boolean firstElemSeen;
		private boolean inArray;
		
		private NodeState(boolean inArray) {
			this.inArray=inArray;
		}
	}

	public JsonWriter() {
		this(new StringWriter());
	}

	public JsonWriter(OutputStream stream) {
		this(new OutputStreamWriter(stream, StreamUtil.DEFAULT_CHARSET));
	}
	
	public JsonWriter(Writer writer) {
		this.writer=writer;
		stateStack.push(new NodeState(false));
	}

	@Override
	public void startDocument() {
		// nothing special
	}

	@Override
	public void endDocument() throws SAXException {
		try {
			writer.flush();
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}

	private void writeSeparatingComma(boolean beforeKey) throws IOException {
		NodeState state = stateStack.peek();
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
			if (value instanceof String) {
				writer.write("\""+value+"\"");
			} else {
				writer.write(value.toString());
			}
		} catch (IOException e) {
			throw new SaxException(e);
		}
	}



	@Override
	public String toString() {
		return writer.toString();
	}

}
