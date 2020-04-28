/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeForward;

public class MessageOutputStreamCap extends MessageOutputStream {

	public MessageOutputStreamCap(INamedObject owner, PipeForward forward) {
		super(owner, null, forward);
	}

	public MessageOutputStreamCap(INamedObject owner, IForwardTarget next) {
		super(owner, null, next);
	}

	@Override
	public Object asNative() {
		if (super.asNative()==null) {
			setRequestStream(new StringWriter());
		}
		return super.asNative();
	}

	@Override
	public OutputStream asStream() throws StreamingException {
		if (super.asNative()==null) {
			setRequestStream(new ByteArrayOutputStream());
		}
		return super.asStream();
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
	public Object getResponse() {
		Object buffer = super.asNative();
		if (buffer==null) {
			return null;
		} else if (buffer instanceof ByteArrayOutputStream) {
			return ((ByteArrayOutputStream)buffer).toByteArray();
		} else {
			return buffer.toString();
		}
	}
	
}
