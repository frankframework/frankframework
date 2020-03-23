/*
   Copyright 2019, 2020 Integration Partners

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

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class Message {
	protected Logger log = LogUtil.getLogger(this);

	private Object request;

	private Message(Object request) {
		if (request instanceof Message) {
			// this code could be reached when this constructor was public and the actual type of the parameter was not known at compile time.
			// e.g. new Message(pipeRunResult.getResult());
			this.request = ((Message)request).asObject();
		} else {
			this.request = request;
		}
	}

	public Message(String request) {
		this((Object)request);
	}

	public Message(byte[] request) {
		this((Object)request);
	}

	public Message(Reader request) {
		this((Object)request);
	}

	public Message(InputStream request) {
		this((Object)request);
	}

	public Message(File request) {
		this((Object)request);
	}

	public Message(URL request) {
		this((Object)request);
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
	public Source asSource() throws IOException, SAXException {
		if (request == null) {
			return null;
		}
		if (request instanceof Source) {
			log.debug("returning Source as Source");
			return (Source) request;
		}
		log.debug("returning as Source");
		return (XmlUtils.inputSourceToSAXSource(asInputSource(), true, false));
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

	public boolean isEmpty() {
		return request == null || request instanceof String && ((String)request).isEmpty();
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

	public static Message asMessage(Object object) {
		if (object==null) {
			return null;
		}
		if (object instanceof Message) {
			return (Message)object;
		}
		return new Message(object);
	}
	
	public static Reader asReader(Object object) throws IOException {
		return asReader(object, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public static Reader asReader(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof Reader) {
			return (Reader)object;
		}
		if (object instanceof Message) {
			return ((Message)object).asReader(defaultCharset);
		}
		return new Message(object).asReader(defaultCharset);
	}
	
	public static InputStream asInputStream(Object object) throws IOException {
		return asInputStream(object, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public static InputStream asInputStream(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof InputStream) {
			return (InputStream)object;
		}
		if (object instanceof Message) {
			return ((Message)object).asInputStream(defaultCharset);
		}
		return new Message(object).asInputStream(defaultCharset);
	}
	
	public static InputSource asInputSource(Object object) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof InputSource) {
			return (InputSource)object;
		}
		if (object instanceof Message) {
			return ((Message)object).asInputSource();
		}
		return new Message(object).asInputSource();
	}
	
	public static Source asSource(Object object) throws IOException, SAXException  {
		if (object==null) {
			return null;
		}
		if (object instanceof Source) {
			return (Source)object;
		}
		if (object instanceof Message) {
			return ((Message)object).asSource();
		}
		return new Message(object).asSource();
	}
	
	public static String asString(Object object) throws IOException {
		return asString(object, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public static String asString(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof String) {
			return (String)object;
		}
		if (object instanceof Message) {
			return ((Message)object).asString();
		}
		return new Message(object).asString();
	}
	
	public static byte[] asByteArray(Object object) throws IOException {
		return asByteArray(object, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}
	public static byte[] asByteArray(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof byte[]) {
			return (byte[])object;
		}
		if (object instanceof Message) {
			return ((Message)object).asByteArray(defaultCharset);
		}
		return new Message(object).asByteArray(defaultCharset);
	}
	
}
