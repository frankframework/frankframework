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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
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
		if (request!=null) {
			this.request = request.asObject();
		}
	}

	public Message(Object request) {
		if (request instanceof Message) {
			this.request = ((Message)request).asObject();
		} else {
			this.request = request;
		}
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
		return asReader(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public Reader asReader(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as Reader");
			return (Reader) request;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader((InputStream) request, defaultCharset);
		}
		if (request instanceof URL) {
			log.debug("returning URL as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(((URL) request).openStream(), defaultCharset);
		}
		if (request instanceof File) {
			log.debug("returning File as Reader");
			try {
				return StreamUtil.getCharsetDetectingInputStreamReader(new FileInputStream((File)request), defaultCharset);
			} catch (IOException e) {
				throw new IOException("Cannot open file ["+((File)request).getPath()+"]");
			}
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(new ByteArrayInputStream((byte[]) request), defaultCharset);
		}
		log.debug("returning String as Reader");
		return new StringReader(request.toString());
	}

	/**
	 * return the request object as a {@link InputStream}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputStream asInputStream() throws IOException {
		return asInputStream(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	
	public InputStream asInputStream(String defaultCharset) throws IOException {
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
		if (request instanceof File) {
			log.debug("returning File as InputStream");
			try {
				return new FileInputStream((File)request);
			} catch (IOException e) {
				throw new IOException("Cannot open file ["+((File)request).getPath()+"]");
			}
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as InputStream");
			return new ReaderInputStream((Reader) request, defaultCharset);
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as InputStream");
			return new ByteArrayInputStream((byte[]) request);
		}
		log.debug("returning String as InputStream");
		return new ByteArrayInputStream(request.toString().getBytes(defaultCharset));
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
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return (new InputSource((Reader) request));
		}
		if (request instanceof String) {
			log.debug("returning String as InputSource");
			return (new InputSource(new StringReader((String) request)));
		}
		log.debug("returning as InputSource");
		return (new InputSource(asInputStream()));
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
		if (request instanceof Reader) {
			log.debug("returning Reader as Source");
			return (new StreamSource((Reader) request));
		}
		if (request instanceof String) {
			log.debug("returning String as Source");
			return (new StreamSource(new StringReader((String) request)));
		}
		log.debug("returning as Source");
		return (new StreamSource(asInputStream()));
	}

	/**
	 * return the request object as a byte array. Has the side effect of preserving the input as byte array.
	 */
	public byte[] asByteArray() throws IOException {
		return asByteArray(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public byte[] asByteArray(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String) {
			return ((String)request).getBytes(defaultCharset);
		}
		if (!(request instanceof byte[])) {
			// save the generated byte array as the request before returning it
			request = StreamUtil.streamToByteArray(asInputStream(defaultCharset), false);
		}
		return (byte[]) request;
	}

	/**
	 * return the request object as a String. Has the side effect of preserving the input as a String.
	 */
	public String asString() throws IOException {
		return asString(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public String asString(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (!(request instanceof String)) {
			// save the generated String as the request before returning it
			request = StreamUtil.readerToString(asReader(defaultCharset), null);
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
