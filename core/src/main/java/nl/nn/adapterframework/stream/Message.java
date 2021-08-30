/*
   Copyright 2019 Integration Partners

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class Message {
	protected Logger log = LogUtil.getLogger(this);

	private Object request;

	public Message(Message request) {
		this.request = request.asObject();
	}

	public Message(Object request) {
		this.request = request;
	}

	/**
	 * Notify the message object that the request object will be used multiple times.
	 * If the request object can only be read one time, it can turn it into a less volatile representation. 
	 * For instance, it could replace an InputStream with a byte array or String.
	 * 
	 * @throws IOException
	 */
	public void preserve() throws IOException {
		if (request == null) {
			return;
		}
		if (request instanceof Reader) {
			log.debug("preserving Reader as String");
			request = StreamUtil.readerToString((Reader) request, null);
			return;
		}
		if (request instanceof InputStream) {
			log.debug("preserving InputStream as byte[]");
			request = StreamUtil.streamToByteArray((InputStream) request, false);
			return;
		}
	}

	public Object asObject() {
		return request;
	}

	/**
	 * return the request object as a {@link Reader}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public Reader asReader() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as Reader");
			return (Reader) request;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader((InputStream) request);
		}
		if (request instanceof URL) {
			log.debug("returning URL as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(((URL) request).openStream());
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(new ByteArrayInputStream((byte[]) request));
		}
		log.debug("returning String as Reader");
		return new StringReader(request.toString());
	}

	/**
	 * return the request object as a {@link InputStream}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputStream asInputStream() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputStream");
			return (InputStream) request;
		}
		if (request instanceof URL) {
			log.debug("returning URL as InputStream");
			return ((URL) request).openStream();
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as InputStream");
			return new ReaderInputStream((Reader) request, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputStream");
			return new ByteArrayInputStream((byte[]) request);
		}
		try {
			log.debug("returning String as InputStream");
			return new ByteArrayInputStream(request.toString().getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
		} catch (UnsupportedEncodingException e) {
			log.warn("unable to parse message using charset ["+StreamUtil.DEFAULT_INPUT_STREAM_ENCODING+"]", e);
			return null;
		}
	}

	/**
	 * return the request object as a {@link InputSource}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputSource asInputSource() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof InputSource) {
			log.debug("returning InputSource as InputSource");
			return (InputSource) request;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputSource");
			return (new InputSource((InputStream) request));
		}
		if (request instanceof URL) {
			log.debug("returning URL as InputSource");
			return (new InputSource(((URL) request).openStream()));
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return (new InputSource((Reader) request));
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputSource");
			return (new InputSource(new ByteArrayInputStream((byte[]) request)));
		}
		log.debug("returning String as InputSource");
		return (new InputSource(new StringReader(request.toString())));
	}

	/**
	 * return the request object as a {@link Source}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public Source asSource() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Source) {
			log.debug("returning Source as Source");
			return (Source) request;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as InputSource");
			return (new StreamSource((InputStream) request));
		}
		if (request instanceof URL) {
			log.debug("returning URL as InputSource");
			return (new StreamSource(((URL) request).openStream()));
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return (new StreamSource((Reader) request));
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputSource");
			return (new StreamSource(new ByteArrayInputStream((byte[]) request)));
		}
		log.debug("returning String as InputSource");
		return (new StreamSource(new StringReader(request.toString())));
	}

	/**
	 * return the request object as a byte array. Has the side effect of preserving the input as byte array.
	 */
	public byte[] asByteArray() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String) {
			return ((String)request).getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		}
		if (!(request instanceof byte[])) {
			request = StreamUtil.streamToByteArray(asInputStream(), false);
		}
		return (byte[]) request;
	}

	/**
	 * return the request object as a String. Has the side effect of preserving the input as a String.
	 */
	public String asString() throws IOException {
		if (request == null) {
			return null;
		}
		if (!(request instanceof String)) {
			request = StreamUtil.readerToString(asReader(), null);
		}
		return (String) request;
	}

	/**
	 * toString can be used to inspect the message. It does not convert the 'request' to a string. 
	 */
	@Override
	public String toString() {
		if (request==null) {
			return "null";
		}
		return request.getClass().getSimpleName()+": "+request.toString();
	}

}
