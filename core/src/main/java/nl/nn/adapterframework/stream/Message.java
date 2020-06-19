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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.Source;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class Message implements Serializable {

	private static final long serialVersionUID = 437863352486501445L;

	protected transient Logger log = LogUtil.getLogger(this);

	private Object request;
	private String charset; // representing a charset of byte typed requests

	private Message(Object request, String charset) {
		if (request instanceof Message) {
			// this code could be reached when this constructor was public and the actual type of the parameter was not known at compile time.
			// e.g. new Message(pipeRunResult.getResult());
			this.request = ((Message)request).asObject();
		} else {
			this.request = request;
		}
		this.charset = charset;
	}

	public Message(String request) {
		this((Object)request, null);
	}

	public Message(byte[] request, String charset) {
		this((Object)request, charset);
	}
	public Message(byte[] request) {
		this((Object)request, null);
	}

	public Message(Reader request) {
		this((Object)request, null);
	}

	public Message(InputStream request, String charset) {
		this((Object)request, charset);
	}
	public Message(InputStream request) {
		this((Object)request, null);
	}

	public Message(File request, String charset) {
		this((Object)request, charset);
	}
	public Message(File request) {
		this((Object)request, null);
	}

	public Message(URL request, String charset) {
		this((Object)request, charset);
	}
	public Message(URL request) {
		this((Object)request, null);
	}
	
	public static Message nullMessage() {
		return new Message((Object)null, null);
	}
	/**
	 * Notify the message object that the request object will be used multiple times.
	 * If the request object can only be read one time, it can turn it into a less volatile representation. 
	 * For instance, it could replace an InputStream with a byte array or String.
	 * 
	 * @throws IOException
	 */
	public void preserve() throws IOException {
		preserve(false);
	}
	private void preserve(boolean deepPreserve) throws IOException {
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
		// if deepPreserve=true, File and URL are also preserved as byte array
		// otherwise we rely on that File and URL can be repeatedly read
		if (deepPreserve && !(request instanceof String || request instanceof byte[])) {
			log.debug("deep preserving as byte[]");
			request = StreamUtil.streamToByteArray(asInputStream(), false);
			return;
		}
	}

	public Object asObject() {
		return request;
	}

	public boolean isBinary() {
		if (request == null) {
			return false;
		}
		return request instanceof InputStream || request instanceof URL || request instanceof File || request instanceof byte[];
	}
	/**
	 * return the request object as a {@link Reader}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public Reader asReader() throws IOException {
		return asReader(null);
	}
	public Reader asReader(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Reader) {
			log.debug("returning Reader as Reader");
			return (Reader) request;
		}
		if (StringUtils.isEmpty(charset)) {
			charset=StringUtils.isNotEmpty(defaultCharset)?defaultCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (request instanceof InputStream) {
			log.debug("returning InputStream as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader((InputStream) request, charset);
		}
		if (request instanceof URL) {
			log.debug("returning URL as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(((URL) request).openStream(), charset);
		}
		if (request instanceof File) {
			log.debug("returning File as Reader");
			try {
				return StreamUtil.getCharsetDetectingInputStreamReader(new FileInputStream((File)request), charset);
			} catch (IOException e) {
				throw new IOException("Cannot open file ["+((File)request).getPath()+"]");
			}
		}
		if (request instanceof byte[]) {
			log.debug("returning byte[] as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(new ByteArrayInputStream((byte[]) request), charset);
		}
		log.debug("returning String as Reader");
		return new StringReader(request.toString());
	}

	/**
	 * return the request object as a {@link InputStream}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputStream asInputStream() throws IOException {
		return asInputStream(null);
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
		if (StringUtils.isEmpty(defaultCharset)) {
			defaultCharset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
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
		return (XmlUtils.inputSourceToSAXSource(asInputSource()));
	}

	/**
	 * return the request object as a byte array. Has the side effect of preserving the input as byte array.
	 */
	public byte[] asByteArray() throws IOException {
		return asByteArray(null);
	}
	public byte[] asByteArray(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof byte[]) {
			return (byte[])request;
		}
		if (StringUtils.isEmpty(defaultCharset)) {
			defaultCharset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (request instanceof String) {
			return ((String)request).getBytes(defaultCharset);
		}
		// save the generated byte array as the request before returning it
		request = StreamUtil.streamToByteArray(asInputStream(defaultCharset), false);
		return (byte[]) request;
	}

	/**
	 * return the request object as a String. Has the side effect of preserving the input as a String.
	 */
	public String asString() throws IOException {
		return asString(null);
	}
	public String asString(String defaultCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String) {
			return (String)request;
		}
		// save the generated String as the request before returning it
		request = StreamUtil.readerToString(asReader(defaultCharset), null);
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
		if (object!=null && object instanceof Message) {
			return (Message)object;
		}
		return new Message(object, null);
	}

	public static Reader asReader(Object object) throws IOException {
		return asReader(object, null);
	}
	public static Reader asReader(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof Reader) {
			return (Reader)object;
		}
		return Message.asMessage(object).asReader(defaultCharset);
	}

	public static InputStream asInputStream(Object object) throws IOException {
		return asInputStream(object, null);
	}
	public static InputStream asInputStream(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof InputStream) {
			return (InputStream)object;
		}
		return Message.asMessage(object).asInputStream(defaultCharset);
	}

	public static InputSource asInputSource(Object object) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof InputSource) {
			return (InputSource)object;
		}
		return Message.asMessage(object).asInputSource();
	}

	public static Source asSource(Object object) throws IOException, SAXException  {
		if (object==null) {
			return null;
		}
		if (object instanceof Source) {
			return (Source)object;
		}
		return Message.asMessage(object).asSource();
	}

	public static String asString(Object object) throws IOException {
		return asString(object, null);
	}
	public static String asString(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof String) {
			return (String)object;
		}
		return Message.asMessage(object).asString();
	}

	public static byte[] asByteArray(Object object) throws IOException {
		return asByteArray(object, null);
	}
	public static byte[] asByteArray(Object object, String defaultCharset) throws IOException {
		if (object==null) {
			return null;
		}
		if (object instanceof byte[]) {
			return (byte[])object;
		}
		return Message.asMessage(object).asByteArray(defaultCharset);
	}

	/*
	 * this method is used by Serializable, to serialize objects to a stream.
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		preserve(true);
		stream.defaultWriteObject();
	}

	/*
	 * this method is used by Serializable, to deserialize objects from a stream.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		log = LogUtil.getLogger(this);
		stream.defaultReadObject();
	}

}
