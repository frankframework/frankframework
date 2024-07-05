/*
   Copyright 2019-2022 WeAreFrank!

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
package org.frankframework.stream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.frankframework.core.INamedObject;
import org.xml.sax.ContentHandler;

public class MessageOutputStreamCap extends MessageOutputStream {

	private Object responseBuffer;
	private Object captureStream;
	private int captureSize;
	private String charset;

	public MessageOutputStreamCap(INamedObject owner) {
		super(owner);
	}

	@Override
	public Object asNative() {
		if (super.asNative()==null) {
			setRequestStream(new StringWriter());
		}
		return super.asNative();
	}

	@Override
	public OutputStream asStream(String charset) throws StreamingException {
		this.charset=charset;
		if (super.asNative()==null) {
			setRequestStream(new ByteArrayOutputStream());
		}
		return super.asStream(charset);
	}

	@Override
	public Writer asWriter() throws StreamingException {
		if (super.asNative()==null) {
			setRequestStream(new StringWriter());
		}
		return super.asWriter();
	}

	@Override
	public ContentHandler asContentHandler() throws StreamingException {
		if (super.asNative()==null) {
			setRequestStream(new StringWriter());
		}
		return super.asContentHandler();
	}

	@Override
	public JsonEventHandler asJsonEventHandler() {
		if (super.asNative()==null) {
			setRequestStream(new StringWriter());
		}
		return super.asJsonEventHandler();
	}

	@Override
	public Message getResponse() {
		if (responseBuffer==null) {
			return null;
		} else if (responseBuffer instanceof ByteArrayOutputStream stream) {
			return new Message(stream.toByteArray(), charset);
		} else {
			return new Message(responseBuffer.toString());
		}
	}

	@Override
	protected void setRequestStream(Object requestStream) {
		super.setRequestStream(requestStream);
		responseBuffer = requestStream;
		installCapture();
	}

	private void installCapture() {
		if (captureStream instanceof Writer writer) {
			super.captureCharacterStream(writer, captureSize);
			return;
		}
		if (captureStream instanceof OutputStream stream) {
			super.captureBinaryStream(stream, captureSize);
		}
	}

	@Override
	public void captureCharacterStream(Writer writer, int maxSize) {
		captureStream = writer;
		captureSize = maxSize;
	}

	@Override
	public void captureBinaryStream(OutputStream outputStream, int maxSize) {
		captureStream = outputStream;
		captureSize = maxSize;
	}
}
