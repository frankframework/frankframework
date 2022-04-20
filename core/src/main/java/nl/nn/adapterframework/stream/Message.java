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
package nl.nn.adapterframework.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Lombok;
import lombok.Setter;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class Message implements Serializable {

	protected transient Logger log = LogUtil.getLogger(this);

	private static final long serialVersionUID = 437863352486501445L;

	private Object request;
	private @Getter String requestClass;
	private @Getter @Setter(AccessLevel.PROTECTED) String charset; // representing a charset of byte typed requests

	private Set<AutoCloseable> resourcesToClose;

	private Message(Object request, String charset, Class<?> requestClass) {
		if (request instanceof Message) {
			// this code could be reached when this constructor was public and the actual type of the parameter was not known at compile time.
			// e.g. new Message(pipeRunResult.getResult());
			this.request = ((Message)request).asObject();
		} else {
			this.request = request;
		}
		this.charset = charset;
		this.requestClass = requestClass!=null ? requestClass.getTypeName() : ClassUtils.nameOf(request);
	}
	private Message(Object request, String charset) {
		this(request, charset, request !=null ? request.getClass() : null);
	}

	public Message(String request) {
		this(request, null);
	}

	public Message(byte[] request, String charset) {
		this((Object)request, charset);
	}
	public Message(byte[] request) {
		this((Object)request, null);
	}

	public Message(Reader request) {
		this(request, null);
	}

	/**
	 * Constructor for Message using InputStream supplier. It is assumed the InputStream can be supplied multiple times.
	 */
	protected Message(ThrowingSupplier<InputStream,Exception> request, String charset, Class<?> requestClass) {
		this((Object)request, charset, requestClass);
	}

	public Message(InputStream request, String charset) {
		this((Object)request, charset);
	}
	public Message(InputStream request) {
		this((Object)request, null);
	}

	public Message(Node request) {
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
			if (isBinary()) {
				log.debug("deep preserving as byte[]");
				request = asByteArray();
			} else {
				log.debug("deep preserving as String");
				request = asString();
			}
		}
	}

	/**
	 * @deprecated Please avoid the use of the raw object.
	 */
	@Deprecated
	public Object asObject() {
		try {
			return (request instanceof ThrowingSupplier) ? asInputStream() : request;
		} catch (IOException e) {
			throw Lombok.sneakyThrow(e);
		}
	}

	public boolean isBinary() {
		return request instanceof InputStream || request instanceof ThrowingSupplier || request instanceof byte[];
	}

	public boolean isRepeatable() {
		return request instanceof String || request instanceof ThrowingSupplier || request instanceof byte[] || request instanceof Node;
	}

	/**
	 * If true, the Message should preferably be read using a streaming method, i.e. asReader() or asInputStream(), to avoid copying it into memory.
	 */
	public boolean requiresStream() {
		return request instanceof InputStream || request instanceof ThrowingSupplier || request instanceof Reader;
	}


	/*
	 * provide close(), but do not implement AutoCloseable, to avoid having to enclose all messages in try-with-resource clauses.
	 */
	public void close() throws Exception {
		try {
			if (request instanceof AutoCloseable) {
				((AutoCloseable)request).close();
				request = null;
			}
		} finally {
			if (resourcesToClose!=null) {
				resourcesToClose.forEach(r -> {
					try {
						r.close();
					} catch (Exception e) {
						log.warn("Could not close resource", e);
					}
				});
			}
		}
	}

	public void closeOnClose(AutoCloseable resource) {
		if (resourcesToClose==null) {
			resourcesToClose = new LinkedHashSet<>();
		}
		resourcesToClose.add(resource);
	}

	public void closeOnCloseOf(PipeLineSession session, INamedObject requester) {
		closeOnCloseOf(session, ClassUtils.nameOf(requester));
	}

	public void closeOnCloseOf(PipeLineSession session, String requester) {
		if (!(request instanceof InputStream || request instanceof Reader) || isScheduledForCloseOnExitOf(session)) {
			return;
		}
		if (log.isDebugEnabled()) log.debug("registering Message ["+this+"] for close on exit");
		if (request instanceof InputStream) {
			request = StreamUtil.onClose((InputStream)request, () -> {
				if (log.isDebugEnabled()) log.debug("closed InputStream and unregistering Message ["+this+"] from close on exit");
				unscheduleFromCloseOnExitOf(session);
			});
		}
		if (request instanceof Reader) {
			request = StreamUtil.onClose((Reader)request, () -> {
				if (log.isDebugEnabled()) log.debug("closed Reader and unregistering Message ["+this+"] from close on exit");
				unscheduleFromCloseOnExitOf(session);
			});
		}
		session.scheduleCloseOnSessionExit(this, request.toString()+" requested by "+requester);
	}

	public boolean isScheduledForCloseOnExitOf(PipeLineSession session) {
		return session.isScheduledForCloseOnExit(this);
	}

	public void unscheduleFromCloseOnExitOf(PipeLineSession session) {
		session.unscheduleCloseOnSessionExit(this);
	}

	private void onExceptionClose(Exception e) {
		try {
			close();
		} catch (Exception e2) {
			e.addSuppressed(e2);
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
			InputStream inputStream = asInputStream();
			try {
				return StreamUtil.getCharsetDetectingInputStreamReader(inputStream, readerCharset);
			} catch (IOException e) {
				onExceptionClose(e);
				throw e;
			} catch (Exception e) {
				onExceptionClose(e);
				throw Lombok.sneakyThrow(e);
			}
		}
		if(request instanceof Node) {
			log.debug("returning Node as Reader");
			return new StringReader(asString());
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
		try {
			if (request == null) {
				return null;
			}
			if (request instanceof InputStream) {
				log.debug("returning InputStream as InputStream");
				return (InputStream) request;
			}
			if (request instanceof ThrowingSupplier) {
				log.debug("returning InputStream from supplier");
				return ((ThrowingSupplier<InputStream,Exception>) request).get();
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
			if(request instanceof Node) {
				log.debug("returning Node as InputStream");
				return new ByteArrayInputStream(asByteArray());
			}
			log.debug("returning String as InputStream");
			return new ByteArrayInputStream(request.toString().getBytes(defaultCharset));
		} catch (IOException e) {
			onExceptionClose(e);
			throw e;
		} catch (Exception e) {
			onExceptionClose(e);
			throw Lombok.sneakyThrow(e);
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
		if (request instanceof Reader) {
			log.debug("returning Reader as InputSource");
			return (new InputSource((Reader) request));
		}
		if (request instanceof String) {
			log.debug("returning String as InputSource");
			return (new InputSource(new StringReader((String) request)));
		}
		log.debug("returning as InputSource");
		if (isBinary()) {
			return new InputSource(asInputStream());
		}
		return new InputSource(asReader());
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
		if (request instanceof Node) {
			log.debug("returning Node as DOMSource");
			return new DOMSource((Node) request);
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
		if (request instanceof Node) {
			try {
				log.warn("returning Node as byte[]; consider to avoid using Node or Document here to reduce memory footprint");
				return XmlUtils.nodeToByteArray((Node) request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert Node to byte[]", e);
			}

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
		if(request instanceof Node) {
			try {
				log.warn("returning Node as String; consider to avoid using Node or Document here to reduce memory footprint");
				return XmlUtils.nodeToString((Node)request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert type Node to String", e);
			}
		}

		// save the generated String as the request before returning it
		String result = StreamUtil.readerToString(asReader(defaultCharset), null);
		if(!isBinary() || !isRepeatable()) {
			request = result;
		}
		return result;
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
		return getRequestClass()+": "+request.toString();
	}

	public static Message asMessage(Object object) {
		if (object instanceof Message) {
			return (Message) object;
		}
		if (object instanceof URL) {
			return new UrlMessage((URL)object, null);
		}
		if (object instanceof File) {
			return new FileMessage((File)object);
		}
		if (object instanceof Path) {
			return new PathMessage((Path)object, null);
		}
		return new Message(object, null);
	}

	public static Object asObject(Object object) {
		if (object instanceof Message) {
			return ((Message) object).asObject();
		}
		return object;
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
		stream.writeObject(getCharset());
		stream.writeObject(request);
		stream.writeObject(requestClass);
	}

	/*
	 * this method is used by Serializable, to deserialize objects from a stream.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		log = LogUtil.getLogger(this);

		charset = (String)stream.readObject();
		request = stream.readObject();
		try {
			Object requestClass = stream.readObject();
			if (requestClass != null) {
				if (requestClass instanceof Class<?>) {
					this.requestClass = ((Class<?>)requestClass).getTypeName();
				} else {
					this.requestClass = requestClass.toString();
				}
			} else {
				this.requestClass = ClassUtils.nameOf(request);
			}
		} catch (Exception e) {
			requestClass = ClassUtils.nameOf(request);
			log.warn("Could not read requestClass, using ClassUtils.nameOf(request) ["+requestClass+"], ("+ClassUtils.nameOf(e)+"): "+e.getMessage());
		}
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
	 * Can be called when {@link #requiresStream()} is true to retrieve a copy of (part of) the stream that is in this
	 * message, after the stream has been closed. Primarily for debugging purposes.
	 */
	public ByteArrayOutputStream captureBinaryStream() throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		captureBinaryStream(result);
		return result;
	}
	public void captureBinaryStream(OutputStream outputStream) throws IOException {
		captureBinaryStream(outputStream, StreamUtil.DEFAULT_STREAM_CAPTURE_LIMIT);
	}
	public void captureBinaryStream(OutputStream outputStream, int maxSize) throws IOException {
		log.debug("creating capture of "+ClassUtils.nameOf(request));
		if (isRepeatable()) {
			log.warn("repeatability of message of type ["+request.getClass().getTypeName()+"] will be lost by capturing stream");
		}
		if (isBinary()) {
			request = StreamUtil.captureInputStream(asInputStream(), outputStream, maxSize, true);
		} else {
			request = StreamUtil.captureReader(asReader(), new OutputStreamWriter(outputStream,StreamUtil.DEFAULT_CHARSET), maxSize, true);
		}
		closeOnClose(outputStream);
	}

	/**
	 * Can be called when {@link #requiresStream()} is true to retrieve a copy of (part of) the stream that is in this
	 * message, after the stream has been closed. Primarily for debugging purposes.
	 *
	 * When isBinary() is true the Message's charset is used when present to create a Reader that reads the InputStream.
	 * When charset not present {@link StreamUtil#DEFAULT_INPUT_STREAM_ENCODING} is used.
	 */
	public StringWriter captureCharacterStream() throws IOException {
		StringWriter result = new StringWriter();
		captureCharacterStream(result);
		return result;
	}
	public void captureCharacterStream(Writer writer) throws IOException {
		captureCharacterStream(writer, StreamUtil.DEFAULT_STREAM_CAPTURE_LIMIT);
	}
	public void captureCharacterStream(Writer writer, int maxSize) throws IOException {
		log.debug("creating capture of "+ClassUtils.nameOf(request));
		if (isRepeatable()) {
			log.warn("repeatability of message of type ["+request.getClass().getTypeName()+"] will be lost by capturing stream");
		}
		if (!isBinary()) {
			request = StreamUtil.captureReader(asReader(), writer, maxSize, true);
		} else {
			String charset = StringUtils.isNotEmpty(getCharset()) ? getCharset() : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			request = StreamUtil.captureInputStream(asInputStream(), new WriterOutputStream(writer, charset), maxSize, true);
		}
		closeOnClose(writer);
	}

}
