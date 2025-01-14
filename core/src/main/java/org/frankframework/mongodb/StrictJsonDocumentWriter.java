/*
   Copyright 2021, 2023-2024 WeAreFrank!

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
package org.frankframework.mongodb;

import java.math.BigDecimal;
import java.util.Stack;

import org.bson.BSONException;
import org.bson.BsonInvalidOperationException;
import org.bson.json.StrictCharacterStreamJsonWriterSettings;
import org.bson.json.StrictJsonWriter;
import org.xml.sax.SAXException;

import org.frankframework.documentbuilder.IArrayBuilder;
import org.frankframework.documentbuilder.INodeBuilder;
import org.frankframework.documentbuilder.IObjectBuilder;

/**
 * StrictJsonWriter to write to FF DocumentBuilder, to generate JSON or XML.
 * <p>
 * Based on org.bson.json.StrictCharacterStreamJsonWriter.
 *
 * @author Gerrit van Brakel
 */
public class StrictJsonDocumentWriter implements StrictJsonWriter {

	private final Stack<AutoCloseable> stack = new Stack<>();
	private final StrictCharacterStreamJsonWriterSettings settings;
	private StrictJsonContext context = new StrictJsonContext(null, JsonContextType.TOP_LEVEL, "");
	private State state = State.INITIAL;
	private int curLength; // not yet handled
	private boolean isTruncated; // not yet handled

	private enum JsonContextType {
		TOP_LEVEL, DOCUMENT, ARRAY,
	}

	private enum State {
		INITIAL, NAME, VALUE, DONE
	}

	private static class StrictJsonContext {
		private final StrictJsonContext parentContext;
		private final JsonContextType contextType;
		private final String indentation;

		StrictJsonContext(final StrictJsonContext parentContext, final JsonContextType contextType,
				final String indentChars) {
			this.parentContext = parentContext;
			this.contextType = contextType;
			this.indentation = parentContext == null ? indentChars : parentContext.indentation + indentChars;
		}
	}

	/**
	 * Construct an instance.
	 *
	 * @param nodeBuilder   the handler to write JSON to.
	 * @param settings the settings to apply to this writer.
	 */
	public StrictJsonDocumentWriter(final INodeBuilder nodeBuilder, final StrictCharacterStreamJsonWriterSettings settings) {
		this.settings = settings;
		stack.push(nodeBuilder);
	}

	/**
	 * Gets the current length of the JSON text.
	 *
	 * @return the current length of the JSON text
	 */
	public int getCurrentLength() {
		return curLength;
	}

	@Override
	public void writeName(final String name) {
		notNull("name", name);
		checkState(State.NAME);
		try {
			stack.push(((IObjectBuilder)stack.peek()).addField(name));
		} catch (SAXException e) {
			throwBSONException(e);
		}

		state = State.VALUE;
	}

	@Override
	public void writeStartObject() {
		if (state != State.INITIAL && state != State.VALUE) {
			throw new BsonInvalidOperationException("Invalid state " + state);
		}
		try {
			INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop();
			stack.push(nodeBuilder);
			stack.push(nodeBuilder.startObject());
		} catch (SAXException e) {
			throwBSONException(e);
		}
		context = new StrictJsonContext(context, JsonContextType.DOCUMENT, settings.getIndentCharacters());
		state = State.NAME;
	}
	@Override
	public void writeStartObject(final String name) {
		writeName(name);
		writeStartObject();
	}

	@Override
	public void writeEndObject() {
		checkState(State.NAME);

		try {
			((IObjectBuilder) stack.pop()).close();
			((INodeBuilder)stack.pop()).close();
		} catch (SAXException e) {
			throwBSONException(e);
		}
		context = context.parentContext;
		if (context.contextType == JsonContextType.TOP_LEVEL) {
			state = State.DONE;
		} else {
			setNextState();
		}
	}

	@Override
	public void writeStartArray() {
		try {
			stack.push(((INodeBuilder)stack.peek()).startArray("item"));
		} catch (SAXException e) {
			throwBSONException(e);
		}
		context = new StrictJsonContext(context, JsonContextType.ARRAY, settings.getIndentCharacters());
		state = State.VALUE;
	}

	@Override
	public void writeStartArray(final String name) {
		writeName(name);
		writeStartArray();
	}

	@Override
	public void writeEndArray() {
		checkState(State.VALUE);

		if (context.contextType != JsonContextType.ARRAY) {
			throw new BsonInvalidOperationException("Can't end an array if not in an array");
		}

		try {
			((IArrayBuilder)stack.pop()).close();
			((INodeBuilder)stack.pop()).close();
		} catch (SAXException e) {
			throwBSONException(e);
		}
		context = context.parentContext;
		if (context.contextType == JsonContextType.TOP_LEVEL) {
			state = State.DONE;
		} else {
			setNextState();
		}
	}


	@Override
	public void writeBoolean(final boolean value) {
		checkState(State.VALUE);

		try (INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop()) {
			nodeBuilder.setValue(value ? Boolean.TRUE: Boolean.FALSE );
		} catch (SAXException e) {
			throwBSONException(e);
		}

		setNextState();
	}
	@Override
	public void writeBoolean(final String name, final boolean value) {
		notNull("name", name);
		writeName(name);
		writeBoolean(value);
	}


	@Override
	public void writeNumber(final String value) {
		notNull("value", value);
		checkState(State.VALUE);
		try {
			try (INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop()) {
				nodeBuilder.setValue(new BigDecimal(value));
			}
		} catch (SAXException e) {
			throwBSONException(e);
		}
		setNextState();
	}
	@Override
	public void writeNumber(final String name, final String value) {
		notNull("name", name);
		notNull("value", value);
		writeName(name);
		writeNumber(value);
	}


	@Override
	public void writeString(final String value) {
		notNull("value", value);
		checkState(State.VALUE);
		try {
			try (INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop()) {
				nodeBuilder.setValue(value);
			}
		} catch (SAXException e) {
			throwBSONException(e);
		}
		setNextState();
	}
	@Override
	public void writeString(final String name, final String value) {
		notNull("name", name);
		notNull("value", value);
		writeName(name);
		writeString(value);
	}

	@Override
	public void writeRaw(final String value) {
		notNull("value", value);
		checkState(State.VALUE);
		try {
			try (INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop()) {
				nodeBuilder.setValue(value);
			}
		} catch (SAXException e) {
			throwBSONException(e);
		}
		setNextState();
	}
	@Override
	public void writeRaw(final String name, final String value) {
		notNull("name", name);
		notNull("value", value);
		writeName(name);
		writeRaw(value);
	}

	@Override
	public void writeNull() {
		checkState(State.VALUE);
		try {
			try (INodeBuilder nodeBuilder = context.contextType == JsonContextType.ARRAY ? ((IArrayBuilder)stack.peek()).addElement() : (INodeBuilder)stack.pop()) {
				nodeBuilder.setValue((String)null);
			}
		} catch (SAXException e) {
			throwBSONException(e);
		}
		setNextState();
	}
	@Override
	public void writeNull(final String name) {
		writeName(name);
		writeNull();
	}


	/**
	 * Return true if the output has been truncated due to exceeding the length
	 * specified in {@link StrictCharacterStreamJsonWriterSettings#getMaxLength()}.
	 *
	 * @return true if the output has been truncated
	 * @since 3.7
	 * @see StrictCharacterStreamJsonWriterSettings#getMaxLength()
	 */
	@Override
	public boolean isTruncated() {
		return isTruncated;
	}


	private void setNextState() {
		if (context.contextType == JsonContextType.ARRAY) {
			state = State.VALUE;
		} else {
			state = State.NAME;
		}
	}

	private void checkState(final State requiredState) {
		if (state != requiredState) {
			throw new BsonInvalidOperationException("Invalid state " + state);
		}
	}

	private void throwBSONException(final SAXException e) {
		throw new BSONException("Wrapping IOException", e);
	}

	private static <T> T notNull(final String name, final T value) {
		if (value == null) {
			throw new IllegalArgumentException(name + " can not be null");
		}
		return value;
	}

}
