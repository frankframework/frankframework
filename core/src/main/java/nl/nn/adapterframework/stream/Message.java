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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
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

import lombok.Getter;
import lombok.Lombok;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageUtils;
import nl.nn.adapterframework.util.StreamCaptureUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class Message implements Serializable {
	public static final long MESSAGE_SIZE_UNKNOWN = -1L;

	protected transient Logger log = LogUtil.getLogger(this);

	private static final long serialVersionUID = 437863352486501445L;

	private Object request;
	private @Getter String requestClass;

	private @Getter Map<String, Object> context;
	private boolean failedToDetermineCharset = false;

	private Set<AutoCloseable> resourcesToClose;

	private Message(Map<String, Object> context, Object request, Class<?> requestClass) {
		if (request instanceof Message) {
			// this code could be reached when this constructor was public and the actual type of the parameter was not known at compile time.
			// e.g. new Message(pipeRunResult.getResult());
			this.request = ((Message) request).asObject();
		} else {
			this.request = request;
		}
		this.context = context != null ? context : new MessageContext();
		this.requestClass = requestClass != null ? ClassUtils.nameOf(requestClass) : ClassUtils.nameOf(request);
	}

	private Message(Map<String, Object> context, Object request) {
		this(context, request, request != null ? request.getClass() : null);
	}

	public Message(String request, Map<String, Object> context) {
		this(context, request);
	}

	public Message(String request) {
		this(request, null);
	}

	public Message(byte[] request, String charset) {
		this(new MessageContext(charset), request);
	}

	public Message(byte[] request, Map<String, Object> context) {
		this(context, request);
	}

	public Message(byte[] request) {
		this(null, request);
	}

	public Message(Reader request, Map<String, Object> context) {
		this(context, request);
	}

	public Message(Reader request) {
		this(null, request);
	}

	/**
	 * Constructor for Message using InputStream supplier. It is assumed the InputStream can be supplied multiple times.
	 */
	protected Message(ThrowingSupplier<InputStream, Exception> request, Map<String, Object> context, Class<?> requestClass) {
		this(context, request, requestClass);
	}

	public Message(InputStream request, String charset) {
		this(new MessageContext(charset), request);
	}

	public Message(InputStream request, Map<String, Object> context) {
		this(context, request);
	}

	public Message(InputStream request) {
		this(null, request);
	}

	public Message(Node request, Map<String, Object> context) {
		this(context, request);
	}

	public Message(Node request) {
		this(null, request);
	}

	public static Message nullMessage() {
		return nullMessage(null);
	}

	public static Message nullMessage(MessageContext context) {
		return new Message(context, null, null);
	}

	public MessageContext copyContext() {
		return new MessageContext(getContext());
	}

	/**
	 * Representing a charset of binary requests
	 *
	 * @return the charset provided when the message was created
	 */
	public String getCharset() {
		return (String) context.get(MessageContext.METADATA_CHARSET);
	}

	/**
	 * If no charset was provided and the requested charset is <code>auto</auto>, try to parse the charset.
	 * If unsuccessful return the default; <code>UTF-8</code>.
	 */
	protected String computeDecodingCharset(String defaultDecodingCharset) throws IOException {
		String decodingCharset = getCharset();

		if (StringUtils.isEmpty(decodingCharset)) {
			decodingCharset = StringUtils.isNotEmpty(defaultDecodingCharset) ? defaultDecodingCharset : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		if (StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(decodingCharset)) {
			Charset charset = null;
			if (!failedToDetermineCharset) {
				charset = MessageUtils.computeDecodingCharset(this);
			}

			// Remove the size, if present
			context.remove(MessageContext.METADATA_SIZE);

			if (charset == null) {
				failedToDetermineCharset = true;
				if (StringUtils.isNotEmpty(defaultDecodingCharset) && !StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(defaultDecodingCharset)) {
					return defaultDecodingCharset;
				}
				return StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			}
			return charset.name();
		}

		return decodingCharset;
	}

	private String computeEncodingCharset(String defaultEncodingCharset) {
		if (StringUtils.isEmpty(defaultEncodingCharset)) {
			defaultEncodingCharset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		return defaultEncodingCharset;
	}

	/**
	 * Notify the message object that the request object will be used multiple times.
	 * If the request object can only be read one time, it can turn it into a less volatile representation.
	 * For instance, it could replace an InputStream with a byte array or String.
	 */
	public void preserve() throws IOException {
		preserve(false);
	}

	private void preserve(boolean deepPreserve) throws IOException {
		if (request == null) {
			return;
		}
		if (request instanceof Reader) {
			log.debug("preserving Reader {} as String", this::getId);
			request = StreamUtil.readerToString((Reader) request, null);
			return;
		}
		if (request instanceof InputStream) {
			log.debug("preserving InputStream {} as byte[]", this::getId);
			request = StreamUtil.streamToByteArray((InputStream) request, false);
			return;
		}
		// if deepPreserve=true, File and URL are also preserved as byte array
		// otherwise we rely on that File and URL can be repeatedly read
		if (deepPreserve && !(request instanceof String || request instanceof byte[])) {
			if (isBinary()) {
				log.debug("deep preserving {} as byte[]", this::getId);
				request = asByteArray();
			} else {
				log.debug("deep preserving {} as String", this::getId);
				request = asString();
			}
		}
	}

	/**
	 * @deprecated Please avoid the use of the raw object.
	 */
	@Deprecated
	public Object asObject() {
		return request;
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
	 * TODO SHOULD IMPLEMENT AUTOCLOSABLE!!
	 * provide close(), but do not implement AutoCloseable, to avoid having to enclose all messages in try-with-resource clauses.
	 */
	public void close() throws Exception {
		try {
			if (request instanceof AutoCloseable) {
				((AutoCloseable) request).close();
				request = null;
			}
		} finally {
			if (resourcesToClose != null) {
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
		if (resourcesToClose == null) {
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
		log.debug("registering Message [{}] for close on exit", this);
		if (request instanceof InputStream) {
			request = StreamUtil.onClose((InputStream) request, () -> {
				log.debug("closed InputStream and unregistering Message [{}] from close on exit", this);
				unscheduleFromCloseOnExitOf(session);
			});
		}
		if (request instanceof Reader) {
			request = StreamUtil.onClose((Reader) request, () -> {
				log.debug("closed Reader and unregistering Message [{}] from close on exit", this);
				unscheduleFromCloseOnExitOf(session);
			});
		}
		session.scheduleCloseOnSessionExit(this, request.toString() + " requested by " + requester);
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

	public Reader asReader(String defaultDecodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Reader) {
			log.debug("returning Reader {} as Reader", this::getId);
			return (Reader) request;
		}
		if (isBinary()) {
			String readerCharset = computeDecodingCharset(defaultDecodingCharset); //Don't overwrite the Message's charset unless it's set to AUTO

			log.debug("returning InputStream {} as Reader", this::getId);
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
		if (request instanceof Node) {
			log.debug("returning Node {} as Reader", this::getId);
			return new StringReader(asString());
		}
		log.debug("returning String {} as Reader", this::getId);
		return new StringReader(request.toString());
	}

	/**
	 * return the request object as a {@link InputStream}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputStream asInputStream() throws IOException {
		return asInputStream(null);
	}

	/**
	 * @param defaultEncodingCharset is only used when the Message object is of character type (String)
	 */
	public InputStream asInputStream(String defaultEncodingCharset) throws IOException {
		try {
			if (request == null) {
				return null;
			}
			if (request instanceof InputStream) {
				log.debug("returning InputStream {} as InputStream", this::getId);
				return (InputStream) request;
			}
			if (request instanceof ThrowingSupplier) {
				log.debug("returning InputStream {} from supplier", this::getId);
				return ((ThrowingSupplier<InputStream, Exception>) request).get();
			}
			if (request instanceof byte[]) {
				log.debug("returning byte[] {} as InputStream", this::getId);
				return new ByteArrayInputStream((byte[]) request);
			}
			if (request instanceof Node) {
				log.debug("returning Node {} as InputStream", this::getId);
				return new ByteArrayInputStream(asByteArray());
			}
			String charset = computeEncodingCharset(defaultEncodingCharset);
			if (request instanceof Reader) {
				log.debug("returning Reader {} as InputStream", this::getId);
				return new ReaderInputStream((Reader) request, charset);
			}
			log.debug("returning String {} as InputStream", this::getId);
			return new ByteArrayInputStream(request.toString().getBytes(charset));
		} catch (IOException e) {
			onExceptionClose(e);
			throw e;
		} catch (Exception e) {
			onExceptionClose(e);
			throw Lombok.sneakyThrow(e);
		}
	}

	/**
	 * Reads the first 10k of a message. If the message does not support markSupported it is wrapped in a buffer.
	 */
	@Nonnull
	public byte[] getMagic() throws IOException {
		return getMagic(10 * 1024);
	}

	/**
	 * Reads the first N bytes message, specified by parameter {@code readLimit}. If the message does not support markSupported it is wrapped in a buffer.
	 *
	 * @param readLimit amount of bytes to read.
	 */
	@Nonnull
	public synchronized byte[] getMagic(int readLimit) throws IOException {
		if (!isBinary()) {
			return readBytesFromCharacterData(readLimit);
		}

		if (request instanceof InputStream) {
			return readBytesFromInputStream(readLimit);
		}
		if (request instanceof byte[]) { //copy of, else we can bump into buffer overflow exceptions
			return Arrays.copyOf((byte[]) request, readLimit);
		}
		if (isRepeatable()) {
			try (InputStream stream = asInputStream()) { //Message is repeatable, close the stream after it's been (partially) read.
				return readBytesFromInputStream(stream, readLimit);
			}
		}

		return new byte[0];
	}

	private byte[] readBytesFromCharacterData(int readLimit) throws IOException {
		if (request instanceof Reader) {
			if (!((Reader) request).markSupported()) {
				request = new BufferedReader((Reader)request, readLimit);
			}
			Reader reader = (Reader) request;
			reader.mark(readLimit);
			try {
				return readBytesFromReader(reader, readLimit);
			} finally {
				reader.reset();
			}
		}

		if (request instanceof String) {
			if (((String) request).isEmpty()) {
				return new byte[0];
			}
			byte[] data = ((String) request).getBytes(StreamUtil.DEFAULT_CHARSET);
			return Arrays.copyOf(data, readLimit);
		}

		if (isRepeatable()) {
			try (Reader reader = asReader()) {
				return readBytesFromReader(reader, readLimit);
			}
		}
		return new byte[0];
	}

	private byte[] readBytesFromReader(Reader reader, int readLimit) throws IOException {
		char[] chars = new char[readLimit];
		int charsRead = reader.read(chars);
		if (charsRead <= 0) {
			return new byte[0];
		}
		return new String(chars, 0, charsRead).getBytes(StreamUtil.DEFAULT_CHARSET);
	}

	private byte[] readBytesFromInputStream(int readLimit) throws IOException {
		if (!((InputStream) request).markSupported()) {
			request = new BufferedInputStream((InputStream) request, readLimit);
		}
		InputStream stream = (InputStream) request;
		stream.mark(readLimit);

		try {
			return readBytesFromInputStream(stream, readLimit);
		} finally {
			stream.reset();
		}
	}

	private byte[] readBytesFromInputStream(InputStream stream, int readLimit) throws IOException {
		byte[] bytes = new byte[readLimit];
		int numRead = stream.read(bytes);
		if (numRead <= 0) {
			return new byte[0];
		}
		if (numRead < readLimit) {
			// move the bytes into a smaller array
			bytes = Arrays.copyOf(bytes, numRead);
		}
		return bytes;
	}

	/**
	 * return the request object as a {@link InputSource}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	public InputSource asInputSource() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof InputSource) {
			log.debug("returning InputSource {} as InputSource", this::getId);
			return (InputSource) request;
		}
		if (request instanceof Reader) {
			log.debug("returning Reader {} as InputSource", this::getId);
			return new InputSource((Reader) request);
		}
		if (request instanceof String) {
			log.debug("returning String {} as InputSource", this::getId);
			return new InputSource(new StringReader((String) request));
		}
		log.debug("returning {} as InputSource", this::getId);
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
			log.debug("returning Source {} as Source", this::getId);
			return (Source) request;
		}
		if (request instanceof Node) {
			log.debug("returning Node {} as DOMSource", this::getId);
			return new DOMSource((Node) request);
		}
		log.debug("returning {} as Source", this::getId);
		return XmlUtils.inputSourceToSAXSource(asInputSource());
	}

	/**
	 * return the request object as a byte array. Has the side effect of preserving the input as byte array.
	 */
	public byte[] asByteArray() throws IOException {
		return asByteArray(null);
	}

	public byte[] asByteArray(String defaultEncodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof byte[]) {
			return (byte[]) request;
		}
		if (request instanceof Node) {
			try {
				log.warn("returning Node {} as byte[]; consider to avoid using Node or Document here to reduce memory footprint", this::getId);
				return XmlUtils.nodeToByteArray((Node) request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert Node " + getId() + " to byte[]", e);
			}
		}
		String charset = computeEncodingCharset(defaultEncodingCharset);
		if (request instanceof String) {
			return ((String) request).getBytes(charset);
		}
		// save the generated byte array as the request before returning it
		request = StreamUtil.streamToByteArray(asInputStream(charset), false);
		return (byte[]) request;
	}

	/**
	 * return the request object as a String. Has the side effect of preserving the input as a String.
	 */
	public String asString() throws IOException {
		return asString(null);
	}

	public String asString(String decodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String) {
			return (String) request;
		}
		if (request instanceof Node) {
			try {
				log.warn("returning Node {} as String; consider to avoid using Node or Document here to reduce memory footprint", this::getId);
				return XmlUtils.nodeToString((Node) request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert type Node " + getId() + " to String", e);
			}
		}

		// save the generated String as the request before returning it
		String result = StreamUtil.readerToString(asReader(decodingCharset), null);
		if (!isBinary() || !isRepeatable()) {
			request = result;
		}
		return result;
	}

	public boolean isNull() {
		return request == null;
	}

	/**
	 * Check if a message is empty. If message size cannot be determined, return {@code false} to be on the safe side although this
	 * might not be strictly correct.
	 *
	 * @return {@code true} if the message is empty, {@false} if message is not empty or if the size cannot be determined up-front.
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	public void toStringPrefix(Writer writer) throws IOException {
		if (context == null || context.isEmpty() || !log.isDebugEnabled()) {
			return;
		}
		writer.write("context:\n");
		for (Entry<String, Object> entry : context.entrySet()) {
			if ("authorization".equalsIgnoreCase(entry.getKey())) {
				String value = StringUtil.hide((String) entry.getValue());
				writer.write(entry.getKey() + ": " + value + "\n");
				continue;
			}
			writer.write(entry.getKey() + ": " + entry.getValue() + "\n");
		}
		writer.write("value:\n");
	}

	public String toStringPrefix() {
		StringWriter result = new StringWriter();
		try {
			toStringPrefix(result);
		} catch (IOException e) {
			result.write("cannot write toStringPrefix: " + e.getMessage());
		}
		return result.toString();
	}

	/**
	 * toString can be used to inspect the message. It does not convert the 'request' to a string.
	 */
	@Override
	public String toString() {
		StringWriter result = new StringWriter();

		try {
			toStringPrefix(result);
			if (request == null) {
				result.write("null");
			} else {
				result.write(getRequestClass() + " " + getId() + ": " + request.toString());
			}
		} catch (IOException e) {
			result.write("cannot write toString: " + e.getMessage());
		}

		return result.toString();
	}

	protected String getId() {
		return "Message[" + Integer.toHexString(hashCode()) + "]";
	}

	public static Message asMessage(Object object) {
		if (object instanceof Message) {
			return (Message) object;
		}
		if (object instanceof URL) {
			return new UrlMessage((URL) object);
		}
		if (object instanceof File) {
			return new FileMessage((File) object);
		}
		if (object instanceof Path) {
			return new PathMessage((Path) object);
		}
		return new Message(null, object);
	}

	@Deprecated
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
		if (object == null) {
			return null;
		}
		if (object instanceof Reader) {
			return (Reader) object;
		}
		return Message.asMessage(object).asReader(defaultCharset);
	}

	public static InputStream asInputStream(Object object) throws IOException {
		return asInputStream(object, null);
	}

	public static InputStream asInputStream(Object object, String defaultCharset) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof InputStream) {
			return (InputStream) object;
		}
		return Message.asMessage(object).asInputStream(defaultCharset);
	}

	public static InputSource asInputSource(Object object) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof InputSource) {
			return (InputSource) object;
		}
		return Message.asMessage(object).asInputSource();
	}

	public static Source asSource(Object object) throws IOException, SAXException {
		if (object == null) {
			return null;
		}
		if (object instanceof Source) {
			return (Source) object;
		}
		return Message.asMessage(object).asSource();
	}

	public static String asString(Object object) throws IOException {
		return asString(object, null);
	}

	public static String asString(Object object, String defaultCharset) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return (String) object;
		}
		return Message.asMessage(object).asString(defaultCharset);
	}

	public static byte[] asByteArray(Object object) throws IOException {
		return asByteArray(object, null);
	}

	public static byte[] asByteArray(Object object, String defaultCharset) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof byte[]) {
			return (byte[]) object;
		}
		return Message.asMessage(object).asByteArray(defaultCharset);
	}

	/**
	 * Check if the message passed is null or empty.
	 *
	 * @param message Message to check. Can be {@code null}.
	 * @return Returns {@code true} if the message is {@code null}, otherwise the result of {@link Message#isEmpty()}.
	 */
	public static boolean isEmpty(Message message) {
		return (message == null || message.isEmpty());
	}

	/**
	 * Check if a message has any data available. This will correctly return {@code true} or {@code false} even
	 * when the message size cannot be determined.
	 * <p/>
	 * However, to do so, some I/O may have to be performed on the message thus making this a
	 * potentially expensive operation which may throw an {@link IOException}.
	 * <p/>
	 * All I/O is done in such a way that no message data is lost (see also {@link Message#getMagic(int)}).
	 *
	 * @param message Message to check. May be {@code null}.
	 * @return Returns {@code false} if the message is {@code null} or of {@link Message#size()} returns 0.
	 * Returns {@code true} if {@link Message#size()} returns a positive value.
	 * If {@link Message#size()} returns {@link Message#MESSAGE_SIZE_UNKNOWN} then checks if any data can
	 * be read via {@link Message#getMagic(int)}.
	 *
	 * @throws IOException Throws an IOException if checking for data in the message throws an IOException.
	 *
	 */
	public static boolean hasDataAvailable(Message message) throws IOException {
		if (Message.isNull(message)) {
			return false;
		}
		long size = message.size();
		if (size == MESSAGE_SIZE_UNKNOWN) {
			return message.getMagic(10).length != 0;
		} else {
			return size != 0;
		}
	}

	public static boolean isNull(Message message) {
		return (message == null || message.isNull());
	}

	/*
	 * this method is used by Serializable, to serialize objects to a stream.
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		preserve(true);

		// Safeguard that "preserve()" did its work well
		// Also, this makes Sonar happy that we're not
		// serializing an incompatible type of object.
		if (request != null && !(request instanceof Serializable)) {
			throw new IllegalArgumentException("This message contains a non-serializable request-object of type " + request.getClass().getName());
		}

		stream.writeObject(getCharset());
		stream.writeObject(request);
		stream.writeObject(requestClass);
	}

	/*
	 * this method is used by Serializable, to deserialize objects from a stream.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		log = LogUtil.getLogger(this);

		String charset = (String) stream.readObject();
		request = stream.readObject();
		try {
			Object requestClass = stream.readObject();
			if (requestClass != null) {
				if (requestClass instanceof Class<?>) {
					this.requestClass = ((Class<?>) requestClass).getTypeName();
				} else {
					this.requestClass = requestClass.toString();
				}
			} else {
				this.requestClass = ClassUtils.nameOf(request);
			}
		} catch (Exception e) {
			requestClass = ClassUtils.nameOf(request);
			log.warn("Could not read requestClass, using ClassUtils.nameOf(request) [" + requestClass + "], (" + ClassUtils.nameOf(e) + "): " + e.getMessage());
		}

		context = new MessageContext().withCharset(charset);
	}

	/**
	 * Note that the size may not be an exact measure of the content size and may or may not account for any encoding of the content.
	 * The size is appropriate for display in a user interface to give the user a rough idea of the size of this part.
	 * 
	 * @return Message size or -1 if it can't determine the size.
	 */
	public long size() {
		if (request == null) {
			return 0L;
		}

		if (context.containsKey(MessageContext.METADATA_SIZE)) {
			return (long) context.get(MessageContext.METADATA_SIZE);
		}

		if (request instanceof String) {
			return ((String) request).length();
		}

		if (request instanceof byte[]) {
			return ((byte[]) request).length;
		}

		if (request instanceof FileInputStream) {
			try {
				FileInputStream fileStream = (FileInputStream) request;
				return fileStream.getChannel().size();
			} catch (IOException e) {
				log.debug("unable to determine size of stream [{}]", ClassUtils.nameOf(request), e);
			}
		}

		if (!(request instanceof InputStream || request instanceof Reader)) {
			//Unable to determine the size of a Stream
			log.debug("unable to determine size of Message [{}]", ClassUtils.nameOf(request));
		}

		return MESSAGE_SIZE_UNKNOWN;
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
		captureBinaryStream(outputStream, StreamCaptureUtils.DEFAULT_STREAM_CAPTURE_LIMIT);
	}

	public void captureBinaryStream(OutputStream outputStream, int maxSize) throws IOException {
		log.debug("creating capture of {}", ClassUtils.nameOf(request));
		if (isRepeatable()) {
			log.warn("repeatability of {} of type [{}] will be lost by capturing stream", this.getId(), request.getClass().getTypeName());
		}
		if (isBinary()) {
			request = StreamCaptureUtils.captureInputStream(asInputStream(), outputStream, maxSize, true);
		} else {
			request = StreamCaptureUtils.captureReader(asReader(), new OutputStreamWriter(outputStream, StreamUtil.DEFAULT_CHARSET), maxSize, true);
		}
		closeOnClose(outputStream);
	}

	/**
	 * Can be called when {@link #requiresStream()} is true to retrieve a copy of (part of) the stream that is in this
	 * message, after the stream has been closed. Primarily for debugging purposes.
	 * <p>
	 * When isBinary() is true the Message's charset is used when present to create a Reader that reads the InputStream.
	 * When charset not present {@link StreamUtil#DEFAULT_INPUT_STREAM_ENCODING} is used.
	 */
	public StringWriter captureCharacterStream() throws IOException {
		StringWriter result = new StringWriter();
		captureCharacterStream(result);
		return result;
	}

	public void captureCharacterStream(Writer writer) throws IOException {
		captureCharacterStream(writer, StreamCaptureUtils.DEFAULT_STREAM_CAPTURE_LIMIT);
	}

	public void captureCharacterStream(Writer writer, int maxSize) throws IOException {
		log.debug("creating capture of {}", ClassUtils.nameOf(request));
		if (isRepeatable()) {
			log.warn("repeatability of {} of type [{}] will be lost by capturing stream", this.getId(), request.getClass().getTypeName());
		}
		if (!isBinary()) {
			request = StreamCaptureUtils.captureReader(asReader(), writer, maxSize, true);
		} else {
			String charset = StringUtils.isNotEmpty(getCharset()) ? getCharset() : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			request = StreamCaptureUtils.captureInputStream(asInputStream(), new WriterOutputStream(writer, charset), maxSize, true);
		}
		closeOnClose(writer);
	}
}
