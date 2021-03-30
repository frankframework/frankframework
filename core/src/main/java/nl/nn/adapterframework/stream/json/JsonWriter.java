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

import org.jsfr.json.JsonSaxHandler;
import org.jsfr.json.PrimitiveHolder;

import lombok.Getter;
import nl.nn.adapterframework.util.StreamUtil;

public class JsonWriter implements JsonSaxHandler {

	private Writer writer;
	private @Getter Exception exception;
	
	private Stack<NodeState> stateStack = new Stack<>();
	
	private class NodeState {
		boolean firstElemSeen;
		boolean inArray;
		
		NodeState(boolean inArray) {
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
	public boolean startJSON() {
		return true;
	}

	@Override
	public boolean endJSON() {
		try {
			writer.flush();
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
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
	public boolean startObject() {
		try {
			writeSeparatingComma(false);
			stateStack.push(new NodeState(false));
			writer.write("{");
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public boolean startObjectEntry(String key) {
		try {
			writeSeparatingComma(true);
			writer.write("\""+key+"\":");
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public boolean endObject() {
		try {
			stateStack.pop();
			writer.write("}");
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public boolean startArray() {
		try {
			writeSeparatingComma(false);
			stateStack.push(new NodeState(true));
			writer.write("[");
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public boolean endArray() {
		try {
			stateStack.pop();
			writer.write("]");
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public boolean primitive(PrimitiveHolder primitiveHolder) {
		try {
			writeSeparatingComma(false);
			Object value = primitiveHolder.getValue();
			if (value instanceof String) {
				writer.write("\""+value+"\"");
			} else {
				writer.write(value.toString());
			}
		} catch (IOException e) {
			exception = e;
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return writer.toString();
	}
	
}
