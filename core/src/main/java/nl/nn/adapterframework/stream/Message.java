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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.transform.Source;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.util.ClassUtils;
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

	public Message(Path request, String charset) {
		this((Object)request, charset);
	}
	public Message(Path request) {
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
		}
	}

	public Object asObject() {
		return request;
	}

	public boolean isBinary() {
		return request instanceof InputStream || request instanceof URL || request instanceof File || request instanceof Path || request instanceof byte[];
	}
	
	public boolean isRepeatable() {
		return request instanceof String || request instanceof URL || request instanceof File || request instanceof Path || request instanceof byte[];
	}
	
	public boolean requiresStream() {
		return request instanceof InputStream || request instanceof URL || request instanceof File || request instanceof Path || request instanceof Reader;
	}
	
	public void closeOnCloseOf(IPipeLineSession session) {
		if (request instanceof InputStream) {
			request = session.scheduleCloseOnSessionExit((InputStream)request);
			return;
		}
		if (request instanceof Reader) {
			request = session.scheduleCloseOnSessionExit((Reader)request);
			return;
		}
	}

	public void unregisterCloseable(IPipeLineSession session) {
		if (request instanceof InputStream || request instanceof Reader) {
			session.unscheduleCloseOnSessionExit((AutoCloseable)request);
		}
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
		if (isBinary()) {
			String readerCharset = charset; //Don't overwrite the Message's charset
			if (StringUtils.isEmpty(readerCharset)) {
				readerCharset=StringUtils.isNotEmpty(defaultCharset)?defaultCharset:StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			}
			log.debug("returning InputStream as Reader");
			return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream(), readerCharset);
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

	/**
	 * @param defaultCharset is only used when the Message object is of character type (String)
	 */
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
				throw new IOException("Cannot open file ["+((File)request).getPath()+"]", e);
			}
		}
		if (request instanceof Path) {
			log.debug("returning Path as InputStream");
			try {
				return Files.newInputStream((Path)request);
			} catch (IOException e) {
				throw new IOException("Cannot open file ["+((Path)request).getFileName()+"]", e);
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
		if (object instanceof Message) {
			return (Message) object;
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
		return Message.asMessage(object).asString(defaultCharset);
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

	public static boolean isEmpty(Message message) {
		return (message == null || message.isEmpty());
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

	/**
	 * @return Message size or -1 if it can't determine the size.
	 */
	public long size() {
		if(request == null) {
			return 0;
		}

		if (request instanceof FileInputStream) {
			try {
				FileInputStream fileStream = (FileInputStream) request;
				return fileStream.getChannel().size();
			} catch (IOException e) {
				log.debug("unable to determine size of stream ["+ClassUtils.nameOf(request)+"]", e);
			}
		}

		if(request instanceof String) {
			return ((String) request).length();
		}
		if (request instanceof File) {
			return ((File) request).length();
		}
		if (request instanceof Path) {
			try {
				return Files.size((Path) request);
			} catch (IOException e) {
				log.debug("unable to determine size of stream ["+ClassUtils.nameOf(request)+"]", e);
			}
		}
		if (request instanceof byte[]) {
			return ((byte[]) request).length;
		}

		if(!(request instanceof InputStream || request instanceof Reader)) {
			//Unable to determine the size of a Stream
			log.debug("unable to determine size of Message ["+ClassUtils.nameOf(request)+"]");
		}
		
		return -1;
	}
	
	/**
	 * Can be called to be able to retrieve a String representation of the inputStream or reader that 
	 * is in this message, after the stream has been closed.
	 */
	public StringWriter captureStream() throws IOException {
		return captureStream(new StringWriter());
	}
	public <W extends Writer> W captureStream(W writer) throws IOException {
		return captureStream(10000, writer);
	}
	public <W extends Writer> W captureStream(int maxSize, W writer) {
		if (!requiresStream()) {
			return null;
		}
		log.debug("creating capture of "+ClassUtils.nameOf(request));
		if (isBinary()) {
			OutputStream stream = new WriterOutputStream(writer, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			try {
				request = new FilterInputStream(asInputStream()) {
					
					int pos;
					
					@Override
					public int read() throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterInputStream.read");
						int result = super.read();
						if (result>=0 && pos++<maxSize) {
							stream.write((byte)result);
							if (log.isTraceEnabled()) log.trace("captured byte ["+result+"]");
						}
						return result;
					}

					@Override
					public int read(byte[] b, int off, int len) throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterInputStream.read");
						int result = super.read(b, off, len);
						if (result>=0 && pos<maxSize) {
							pos+=result;
							stream.write(b, off, result);
							if (log.isTraceEnabled()) log.trace("captured ["+result+"] bytes");
						}
						return result;
					}

					@Override
					public void close() throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterInputStream.close");
						if (available()>0) {
							if (log.isTraceEnabled()) log.trace("bytes available at close");
							stream.write("(--MoreBytesAvailable--)[".getBytes());
							read(new byte[1000]);
							stream.write("]".getBytes());
							if (read()>0) {
								stream.write("...".getBytes());
							}
						}
						super.close();
						stream.close();
					}

				};
			} catch (IOException e) {
				log.warn("Cannot capture stream", e);
				return null;
			}
		} else {
			try {
				request = new FilterReader(asReader()) {

					int pos;
					
					@Override
					public int read() throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterReader.read");
						int result = super.read();
						if (result>=0 && pos++<maxSize) {
							writer.write((char)result);
						}
						return result;
					}

					@Override
					public int read(char[] cbuf, int off, int len) throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterReader.read");
						int result = super.read(cbuf, off, len);
						if (result>=0 && pos<maxSize) {
							pos+=result;
							writer.write(cbuf, off, result);
						}
						return result;
					}

					@Override
					public void close() throws IOException {
						if (log.isTraceEnabled()) log.trace("FilterReader.close");
						try {
							char buf[] = new char[1000];
							int len = read(buf);
							if (len>0) {
								writer.write("(--read "+len+" more characters at close() --)");
							}
						} catch (Exception e) {
							log.warn("Caught exception while trying to read more characters at close()", e);
						}
						super.close();
					}
				};
			} catch (IOException e) {
				log.warn("Cannot capture reader", e);
				return null;
			}
			
		}
		return writer;
	}
	
}
