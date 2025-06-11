/*
   Copyright 2019-2024 WeAreFrank!

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
import java.io.Closeable;
import java.io.EOFException;
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
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import lombok.Getter;
import lombok.Lombok;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.MessageUtils;
import nl.nn.adapterframework.util.StreamCaptureUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringUtil;
import nl.nn.adapterframework.util.XmlUtils;

public class Message implements Serializable, Closeable {
	public static final long MESSAGE_SIZE_UNKNOWN = -1L;
	public static final long MESSAGE_MAX_IN_MEMORY_DEFAULT = 512L * 1024L;
	private static final String MESSAGE_MAX_IN_MEMORY_PROPERTY = "message.max.memory.size";

	private static final Logger LOG = LogManager.getLogger(Message.class);

	private static final long serialVersionUID = 437863352486501445L;

	private @Nullable Object request;
	private @Getter @Nonnull String requestClass;

	private @Getter @Nonnull MessageContext context;
	private boolean failedToDetermineCharset = false;

	private Set<AutoCloseable> resourcesToClose;

	private Message(@Nonnull MessageContext context, @Nullable Object request, @Nullable Class<?> requestClass) {
		if (request instanceof Message) {
			// this code could be reached when this constructor was public and the actual type of the parameter was not known at compile time.
			// e.g. new Message(pipeRunResult.getResult());
			throw new IllegalArgumentException("Cannot pass object of type Message to Message constructor");
		} else {
			this.request = request;
		}
		this.context = context;
		this.requestClass = requestClass != null ? ClassUtils.nameOf(requestClass) : ClassUtils.nameOf(request);
	}

	private Message(@Nonnull MessageContext context, Object request) {
		this(context, request, request != null ? request.getClass() : null);
	}

	public Message(String request, @Nonnull MessageContext context) {
		this(context, request);
	}

	public Message(String request) {
		this(new MessageContext(), request);
	}

	public Message(byte[] request, String charset) {
		this(new MessageContext(charset), request);
	}

	public Message(byte[] request, @Nonnull MessageContext context) {
		this(context, request);
	}

	public Message(byte[] request) {
		this(new MessageContext(), request);
	}

	public Message(Reader request, @Nonnull MessageContext context) {
		this(context, request);
	}

	public Message(Reader request) {
		this(new MessageContext(), request);
	}

	/**
	 * Constructor for Message using InputStream supplier. It is assumed the InputStream can be supplied multiple times.
	 */
	protected Message(ThrowingSupplier<InputStream, Exception> request, @Nonnull MessageContext context, Class<?> requestClass) {
		this(context, request, requestClass);
	}

	/**
	 * Constructor for Message using a {@link SerializableFileReference}.
	 *
	 * @param request Request as {@link SerializableFileReference}
	 * @param context {@link MessageContext}
	 * @param requestClass {@link Class} of the original request from which the {@link SerializableFileReference} request was created
	 */
	protected Message(SerializableFileReference request, @Nonnull MessageContext context, Class<?> requestClass) {
		this(context, request, requestClass);
	}

	public Message(InputStream request, String charset) {
		this(new MessageContext(charset), request);
	}

	public Message(InputStream request, @Nonnull MessageContext context) {
		this(context, request);
	}

	public Message(InputStream request) {
		this(new MessageContext(), request);
	}

	public Message(Node request, @Nonnull MessageContext context) {
		this(context, request);
	}

	public Message(Node request) {
		this(new MessageContext(), request);
	}

	@Nonnull
	public static Message nullMessage() {
		return nullMessage(new MessageContext());
	}

	@Nonnull
	public static Message nullMessage(@Nonnull MessageContext context) {
		return new Message(context, null, null);
	}

	@Nonnull
	public MessageContext copyContext() {
		return new MessageContext(getContext());
	}

	/**
	 * Representing a charset of binary requests
	 *
	 * @return the charset provided when the message was created
	 */
	@Nullable
	public String getCharset() {
		return (String) context.get(MessageContext.METADATA_CHARSET);
	}

	/**
	 * If no charset was provided and the requested charset is <code>auto</auto>, try to parse the charset.
	 * If unsuccessful return the default; <code>UTF-8</code>.
	 */
	@Nonnull
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

			// Remove the size, if present, when the charset changes!
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

	@Nonnull
	private String getEncodingCharset(String defaultEncodingCharset) {
		if (StringUtils.isEmpty(defaultEncodingCharset)) {
			defaultEncodingCharset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		return defaultEncodingCharset;
	}

	/**
	 * Notify the message object that the request object will be used multiple times.
	 * If the request object can only be read one time, it can turn it into a less volatile representation.
	 * For instance, it could replace an InputStream with a byte array or String.
	 *
	 * @throws IOException Throws IOException if the Message can not be read or writing fails.
	 */
	public void preserve() throws IOException {
		preserve(false);
	}

	private void preserve(boolean deepPreserve) throws IOException {
		if (request == null) {
			return;
		}
		if (request instanceof SerializableFileReference) {
			return;
		}

		long requestSize = size();
		if (requestSize == MESSAGE_SIZE_UNKNOWN || requestSize > AppConstants.getInstance().getLong(MESSAGE_MAX_IN_MEMORY_PROPERTY, MESSAGE_MAX_IN_MEMORY_DEFAULT)) {
			preserveToDisk(deepPreserve);
		} else {
			preserveToMemory(deepPreserve);
		}
	}

	private void preserveToMemory(boolean deepPreserve) throws IOException {
		if (request instanceof SerializableFileReference) {
			// Should not happen but just in case.
			return;
		}
		if (request instanceof Reader) {
			LOG.debug("preserving Reader {} as String", this::getObjectId);
			request = StreamUtil.readerToString((Reader) request, null);
			return;
		}
		if (request instanceof InputStream) {
			LOG.debug("preserving InputStream {} as byte[]", this::getObjectId);
			request = StreamUtil.streamToByteArray((InputStream) request, false);
			return;
		}
		// if deepPreserve=true, File and URL are also preserved as byte array
		// otherwise we rely on that File and URL can be repeatedly read
		if (deepPreserve && !(request instanceof String || request instanceof byte[])) {
			if (isBinary()) {
				LOG.debug("deep preserving {} as byte[]", this::getObjectId);
				request = asByteArray();
			} else {
				LOG.debug("deep preserving {} as String", this::getObjectId);
				request = asString();
			}
		}
	}

	/**
	 * Preserve message to disk.
	 *
	 * @throws IOException Throws {@link IOException} if the Message cannot be read, or no temporary file can be written to.
	 */
	private void preserveToDisk(boolean deepPreserve) throws IOException {
		if (request instanceof SerializableFileReference) {
			// Should not happen but just in case.
			return;
		}
		if (request instanceof Reader) {
			LOG.debug("preserving Reader {} as SerializableFileReference", this::getObjectId);
			request = SerializableFileReference.of((Reader) request, computeDecodingCharset(getCharset()));
		} else if (request instanceof InputStream) {
			LOG.debug("preserving InputStream {} as SerializableFileReference", this::getObjectId);
			request = SerializableFileReference.of((InputStream) request);
		} else if (request instanceof String) {
			request = SerializableFileReference.of((String) request, computeDecodingCharset(getCharset()));
		} else if (request instanceof byte[]) {
			request = SerializableFileReference.of((byte[]) request);
		} else if (deepPreserve) {
			if (isBinary()) {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				request = SerializableFileReference.of(asInputStream());
			} else {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				request = SerializableFileReference.of(asReader(), computeDecodingCharset(getCharset()));
			}
		}
	}

	/**
	 * @deprecated Please avoid the use of the raw object.
	 */
	@Deprecated
	@Nullable
	public Object asObject() {
		return request;
	}

	public boolean isBinary() {
		if (request instanceof SerializableFileReference) {
			return ((SerializableFileReference) request).isBinary();
		}

		return request instanceof InputStream || request instanceof ThrowingSupplier || request instanceof byte[];
	}

	public boolean isRepeatable() {
		return request instanceof String || request instanceof ThrowingSupplier || request instanceof byte[] || request instanceof Node || request instanceof SerializableFileReference;
	}

	/**
	 * If true, the Message should preferably be read using a streaming method, i.e. asReader() or asInputStream(), to avoid copying it into memory.
	 */
	public boolean requiresStream() {
		return request instanceof InputStream || request instanceof ThrowingSupplier || request instanceof Reader || request instanceof SerializableFileReference;
	}

	@Override
	public void close() throws IOException {
		try {
			if (request instanceof AutoCloseable) {
				try {
					((AutoCloseable) request).close();
				} catch (Exception e) {
					LOG.warn("Could not close request", e);
				}
			}
			request = null;
		} finally {
			if (resourcesToClose != null) {
				resourcesToClose.forEach(r -> {
					try {
						r.close();
					} catch (Exception e) {
						LOG.warn("Could not close resource", e);
					}
				});
			}
		}
	}

	private void closeOnClose(@Nonnull AutoCloseable resource) {
		if (resourcesToClose == null) {
			resourcesToClose = new LinkedHashSet<>();
		}
		resourcesToClose.add(resource);
	}

	public void closeOnCloseOf(@Nonnull PipeLineSession session, INamedObject requester) {
		closeOnCloseOf(session, ClassUtils.nameOf(requester));
	}

	public void closeOnCloseOf(@Nonnull PipeLineSession session, String requester) {
		if (this.request == null || isScheduledForCloseOnExitOf(session)) {
			return;
		}
		LOG.debug("registering Message [{}] for close on exit", this);
		session.scheduleCloseOnSessionExit(this, StringUtils.truncate(request.toString(), 100) + " requested by " + requester);
	}

	public boolean isScheduledForCloseOnExitOf(@Nonnull PipeLineSession session) {
		return session.isScheduledForCloseOnExit(this);
	}

	public void unscheduleFromCloseOnExitOf(@Nonnull PipeLineSession session) {
		session.unscheduleCloseOnSessionExit(this);
		if (request instanceof AutoCloseable) {
			session.unscheduleCloseOnSessionExit((AutoCloseable) request);
		}
	}

	private void onExceptionClose(@Nonnull Exception e) {
		try {
			close();
		} catch (Exception e2) {
			e.addSuppressed(e2);
		}
	}

	/**
	 * Return a {@link Reader} backed by the data in this message. {@link Reader#markSupported()} is guaranteed to be true for the returned stream.
	 * Should not be called more than once, unless the request is {@link #isRepeatable() repeatable} or the Reader is reset to its starting position. If
	 * the message is not {@link #isRepeatable() repeatable} then {@link #preserve()} can be called on the message to make it repeatable.
	 */
	@Nullable
	public Reader asReader() throws IOException {
		return asReader(null);
	}

	/**
	 * Return a {@link Reader} backed by the data in this message. {@link Reader#markSupported()} is guaranteed to be true for the returned stream.
	 * Should not be called more than once, unless the request is {@link #isRepeatable() repeatable} or the Reader is reset to its starting position. If
	 * the message is not {@link #isRepeatable() repeatable} then {@link #preserve()} can be called on the message to make it repeatable.
	 *
	 * @param defaultDecodingCharset is only used when {@link #isBinary()} is {@code true}.
	 */
	@Nullable
	public Reader asReader(@Nullable String defaultDecodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof Reader) {
			Reader reader = (Reader) request;
			if (!reader.markSupported()) {
				reader = new BufferedReader(reader);
				request = reader;
			}
			LOG.debug("returning Reader {} as Reader", this::getObjectId);
			return (Reader) request;
		}
		if (request instanceof SerializableFileReference && !((SerializableFileReference)request).isBinary()) {
			LOG.debug("returning SerializableFileReference {} as Reader", this::getObjectId);
			return ((SerializableFileReference)request).getReader();
		}
		if (isBinary()) {
			String readerCharset = computeDecodingCharset(defaultDecodingCharset); //Don't overwrite the Message's charset unless it's set to AUTO

			LOG.debug("returning InputStream {} as Reader", this::getObjectId);
			InputStream inputStream = asInputStream();
			try {
				BufferedReader reader = StreamUtil.getCharsetDetectingInputStreamReader(inputStream, readerCharset);
				if (this.request instanceof InputStream) {
					this.request = reader;
				}
				return reader;
			} catch (IOException e) {
				onExceptionClose(e);
				throw e;
			} catch (Exception e) {
				onExceptionClose(e);
				throw Lombok.sneakyThrow(e);
			}
		}
		if (request instanceof Node) {
			LOG.debug("returning Node {} as Reader", this::getObjectId);
			return new StringReader(Objects.requireNonNull(asString()));
		}
		LOG.debug("returning String {} as Reader", this::getObjectId);
		return new StringReader(request.toString());
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 * Should not be called more than once, unless the request is {@link #isRepeatable() repeatable} or the InputStream is reset to its starting position. If
	 * the message is not {@link #isRepeatable() repeatable} then {@link #preserve()} can be called on the message to make it repeatable.
	 */
	@Nullable
	public InputStream asInputStream() throws IOException {
		return asInputStream(null);
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 * Should not be called more than once, unless the request is {@link #isRepeatable() repeatable} or the InputStream is reset to its starting position. If
	 * the message is not {@link #isRepeatable() repeatable} then {@link #preserve()} can be called on the message to make it repeatable.
	 *
	 * @param defaultEncodingCharset is only used when the Message object is of character type (String)
	 */
	@Nullable
	public InputStream asInputStream(@Nullable String defaultEncodingCharset) throws IOException {
		try {
			if (request == null) {
				return null;
			}
			if (request instanceof InputStream) {
				InputStream stream = (InputStream) request;
				if (!stream.markSupported()) {
					stream = new BufferedInputStream(stream);
					request = stream;
				}
				LOG.debug("returning InputStream {} as InputStream", this::getObjectId);
				return (InputStream) request;
			}
			if (request instanceof SerializableFileReference) {
				LOG.debug("returning InputStream {} from SerializableFileReference", this::getObjectId);
				return ((SerializableFileReference)request).getInputStream();
			}
			if (request instanceof ThrowingSupplier) {
				LOG.debug("returning InputStream {} from supplier", this::getObjectId);
				@SuppressWarnings("unchecked")
				InputStream is = ((ThrowingSupplier<InputStream, Exception>) request).get();
				if (is.markSupported()) {
					return is;
				} else {
					return new BufferedInputStream(is);
				}
			}
			if (request instanceof byte[]) {
				LOG.debug("returning byte[] {} as InputStream", this::getObjectId);
				return new ByteArrayInputStream((byte[]) request);
			}
			if (request instanceof Node) {
				LOG.debug("returning Node {} as InputStream", this::getObjectId);
				return new ByteArrayInputStream(asByteArray());
			}
			String charset = getEncodingCharset(defaultEncodingCharset);
			if (request instanceof Reader) {
				Reader reader = (Reader) request;
				LOG.debug("returning Reader {} as InputStream", this::getObjectId);
				BufferedInputStream inputStream = new BufferedInputStream(new ReaderInputStream(reader, charset));
				this.request = inputStream;
				return inputStream;
			}
			LOG.debug("returning String {} as InputStream", this::getObjectId);
			return new ByteArrayInputStream(request.toString().getBytes(charset));
		} catch (IOException e) {
			onExceptionClose(e);
			throw e;
		} catch (Exception e) {
			onExceptionClose(e);
			throw Lombok.sneakyThrow(e);
		}
	}

	@Nonnull
	public synchronized String peek(int readLimit) throws IOException {
		Reader r = asReader();
		if (r == null) {
			return "";
		}
		r.mark(readLimit);
		try {
			char[] buffer = new char[readLimit];
			int len = r.read(buffer);
			if (len <= 0) {
				return "";
			}
			return new String(buffer, 0, len);
		} finally {
			r.reset();
		}
	}

	/**
	 * return the request object as a {@link InputSource}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	@Nullable
	public InputSource asInputSource() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof InputSource) {
			LOG.debug("returning InputSource {} as InputSource", this::getObjectId);
			return (InputSource) request;
		}
		if (request instanceof Reader) {
			LOG.debug("returning Reader {} as InputSource", this::getObjectId);
			return new InputSource((Reader) request);
		}
		if (request instanceof String) {
			LOG.debug("returning String {} as InputSource", this::getObjectId);
			return new InputSource(new StringReader((String) request));
		}
		LOG.debug("returning {} as InputSource", this::getObjectId);
		if (isBinary() && getCharset() == null) { //When a charset is present it should be used.
			return new InputSource(asInputStream());
		}
		return new InputSource(asReader());
	}

	/**
	 * return the request object as a {@link Source}. Should not be called more than once, if request is not {@link #preserve() preserved}.
	 */
	@Nullable
	public Source asSource() throws IOException, SAXException {
		if (request == null) {
			return null;
		}
		if (request instanceof Source) {
			LOG.debug("returning Source {} as Source", this::getObjectId);
			return (Source) request;
		}
		if (request instanceof Node) {
			LOG.debug("returning Node {} as DOMSource", this::getObjectId);
			return new DOMSource((Node) request);
		}
		LOG.debug("returning {} as Source", this::getObjectId);
		return XmlUtils.inputSourceToSAXSource(asInputSource());
	}

	/**
	 * return the request object as a byte array. Has the side effect of preserving the input as byte array.
	 */
	@Nullable
	public byte[] asByteArray() throws IOException {
		return asByteArray(null);
	}

	@Nullable
	public byte[] asByteArray(@Nullable String defaultEncodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof byte[]) {
			return (byte[]) request;
		}
		if (request instanceof Node) {
			try {
				LOG.debug("returning Node {} as byte[]", this::getObjectId);
				return XmlUtils.nodeToByteArray((Node) request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert Node " + getObjectId() + " to byte[]", e);
			}
		}
		String charset = getEncodingCharset(defaultEncodingCharset);
		if (request instanceof String) {
			return ((String) request).getBytes(charset);
		}
		if (request instanceof ThrowingSupplier) { // asInputStream handles the exception and cast for us.
			LOG.debug("returning InputStream {} from supplier", this::getObjectId);
			return StreamUtil.streamToByteArray(asInputStream(), false, (int) size() + 32);
		}
		// save the generated byte array as the request before returning it
		// Specify initial capacity a little larger than file-size just as extra safeguard we do not re-allocate buffer.
		request = StreamUtil.streamToByteArray(asInputStream(charset), false, (int) size() + 32);
		return (byte[]) request;
	}

	/**
	 * return the request object as a String. Has the side effect of preserving the input as a String.
	 */
	@Nullable
	public String asString() throws IOException {
		return asString(null);
	}

	@Nullable
	public String asString(@Nullable String decodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String) {
			return (String) request;
		}
		if (request instanceof Node) {
			try {
				LOG.debug("returning Node {} as String", this::getObjectId);
				return XmlUtils.nodeToString((Node) request);
			} catch (TransformerException e) {
				throw new IOException("Could not convert type Node " + getObjectId() + " to String", e);
			}
		}

		// save the generated String as the request before returning it
		// Specify initial capacity a little larger than file-size just as extra safeguard we do not re-allocate buffer.
		String result = StreamUtil.readerToString(asReader(decodingCharset), null, false, (int) size() + 32);
		if (!(request instanceof SerializableFileReference) && (!isBinary() || !isRepeatable())) {
			if (request instanceof AutoCloseable) {
				try {
					((AutoCloseable) request).close();
				} catch (Exception e) {
					LOG.info("could not close request of type [{}], inside message {}. Message: {}", requestClass, this, e.getMessage());
				}
			}
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

	private void toStringPrefix(StringBuilder writer) {
		if (context.isEmpty() || !LOG.isDebugEnabled()) {
			return;
		}
		writer.append("context:\n");
		for (Entry<String, Object> entry : context.entrySet()) {
			Object value = entry.getValue();
			if ("authorization".equalsIgnoreCase(entry.getKey())) {
				value = StringUtil.hide((String) value);
			}
			writer.append(entry.getKey()).append(": ").append(value).append("\n");
		}
		writer.append("value:\n");
	}

	/**
	 * toString can be used to inspect the message. It does not convert the 'request' to a string.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		toStringPrefix(result);
		result.append(getObjectId());

		if (request != null) {
			result.append(": [").append(request).append("]");
		}


		return result.toString();
	}

	/**
	 * Returns the message identifier and which resource class it represents
	 * @return Message[1234abcd:ByteArrayInputStream]
	 */
	public String getObjectId() {
		StringBuilder result = new StringBuilder("Message[");
		result.append(Integer.toHexString(hashCode()));
		result.append(":");
		result.append(getRequestClass());

		return result.append("]").toString();
	}

	public static Message asMessage(Object object) {
		if (object == null) {
			return nullMessage();
		}
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
		if (object instanceof MessageWrapper) {
			return ((MessageWrapper<?>)object).getMessage();
		}
		if (object instanceof RawMessageWrapper) {
			throw new IllegalArgumentException("Raw message extraction / wrapping should be done via Listener.");
		}
		return new Message(new MessageContext(), object);
	}

	/**
	 * Convert an object to a string. Does not close object when it is of type Message or MessageWrapper.
	 */
	public static String asString(Object object) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof String) {
			return (String) object;
		}
		if (object instanceof Message) {
			return ((Message) object).asString();
		}
		if (object instanceof MessageWrapper) {
			return ((MessageWrapper<?>) object).getMessage().asString();
		}
		// In other cases, message can be closed directly after converting to String.
		try (Message message = Message.asMessage(object)) {
			return message.asString();
		}
	}

	/**
	 * Convert an object to a byte array. Does not close object when it is of type Message or MessageWrapper.
	 */
	public static byte[] asByteArray(Object object) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof byte[]) {
			return (byte[]) object;
		}
		if (object instanceof String) {
			return ((String) object).getBytes();
		}
		if (object instanceof Message) {
			return ((Message) object).asByteArray();
		}
		if (object instanceof MessageWrapper) {
			return ((MessageWrapper<?>) object).getMessage().asByteArray();
		}
		// In other cases, message can be closed directly after converting to byte array.
		try (Message message = Message.asMessage(object)) {
			return message.asByteArray();
		}
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
	 * All I/O is done in such a way that no message data is lost .
	 *
	 * @param message Message to check. May be {@code null}.
	 * @return Returns {@code false} if the message is {@code null} or of {@link Message#size()} returns 0.
	 * 		Returns {@code true} if {@link Message#size()} returns a positive value.
	 * 		If {@link Message#size()} returns {@link Message#MESSAGE_SIZE_UNKNOWN} then checks if any data can
	 * 		be read. Data read is pushed back onto the stream.
	 * @throws IOException Throws an IOException if checking for data in the message throws an IOException.
	 *
	 */
	public static boolean hasDataAvailable(Message message) throws IOException {
		if (Message.isNull(message)) {
			return false;
		}
		long size = message.size();
		if (size != MESSAGE_SIZE_UNKNOWN) {
			return size != 0;
		}
		if (message.isBinary()) {
			return checkIfStreamHasData(message.asInputStream());
		} else {
			return checkIfReaderHasData(message.asReader());
		}
	}

	private static boolean checkIfReaderHasData(Reader r) throws IOException {
		if (r == null) {
			return false;
		}
		r.mark(1);
		try {
			return r.read() != -1;
		} catch (EOFException e) {
			return false;
		}  finally {
			r.reset();
		}
	}

	private static boolean checkIfStreamHasData(InputStream is) throws IOException {
		if (is == null) {
			return false;
		}
		is.mark(1);
		try {
			return is.read() != -1;
		} catch (EOFException e) {
			return false;
		} finally {
			is.reset();
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
		stream.writeObject(context);
	}

	/*
	 * this method is used by Serializable, to deserialize objects from a stream.
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		String charset = (String) stream.readObject();
		request = stream.readObject();
		try {
			Object requestClassFromStream = stream.readObject();
			if (requestClassFromStream != null) {
				if (requestClassFromStream instanceof Class<?>) {
					this.requestClass = ((Class<?>) requestClassFromStream).getTypeName();
				} else {
					this.requestClass = requestClassFromStream.toString();
				}
			} else {
				this.requestClass = ClassUtils.nameOf(request);
			}
		} catch (Exception e) {
			requestClass = ClassUtils.nameOf(request);
			LOG.warn("Could not read requestClass, using ClassUtils.nameOf(request) [{}], ({}): {}", ()->requestClass, ()->ClassUtils.nameOf(e),  e::getMessage);
		}
		MessageContext contextFromStream;
		try {
			contextFromStream = (MessageContext) stream.readObject();
		} catch (Exception e) {
			// Old version of object, does not yet have the MessageContext stored?
			LOG.debug("Could not read MessageContext of message {}, old format message? Exception: {}", requestClass, e.getMessage());
			contextFromStream = null;
		}
		if (contextFromStream == null) {
			contextFromStream = new MessageContext().withCharset(charset);
		}
		context = contextFromStream;
	}

	/**
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
			long size = ((String) request).getBytes(StreamUtil.DEFAULT_CHARSET).length;
			getContext().put(MessageContext.METADATA_SIZE, size);
			return size;
		}

		if (request instanceof byte[]) {
			return ((byte[]) request).length;
		}

		if (request instanceof SerializableFileReference) {
			return ((SerializableFileReference)request).getSize();
		}

		if (request instanceof FileInputStream) {
			try {
				FileInputStream fileStream = (FileInputStream) request;
				return fileStream.getChannel().size();
			} catch (IOException e) {
				LOG.debug("unable to determine size of stream [{}], error: {}", (Supplier<?>) ()->ClassUtils.nameOf(request), (Supplier<?>) e::getMessage, e);
			}
		}

		if (!(request instanceof InputStream || request instanceof Reader)) {
			//Unable to determine the size of a Stream
			LOG.debug("unable to determine size of Message [{}]", ()->ClassUtils.nameOf(request));
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
		LOG.debug("creating capture of {}", ClassUtils.nameOf(request));
		if (isRepeatable()) {
			LOG.warn("repeatability of {} of type [{}] will be lost by capturing stream", this.getObjectId(), request.getClass().getTypeName());
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
		LOG.debug("creating capture of {}", ()->ClassUtils.nameOf(request));
		if (isRepeatable()) {
			LOG.warn("repeatability of {} of type [{}] will be lost by capturing stream", this.getObjectId(), request.getClass().getTypeName());
		}
		if (!isBinary()) {
			request = StreamCaptureUtils.captureReader(asReader(), writer, maxSize, true);
		} else {
			String charset = StringUtils.isNotEmpty(getCharset()) ? getCharset() : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			request = StreamCaptureUtils.captureInputStream(asInputStream(), new WriterOutputStream(writer, charset), maxSize, true);
		}
		closeOnClose(writer);
	}

	/**
	 * Creates a copy of this Message object.
	 * <p>
	 *     <b>NB:</b> To copy the underlying value of the message object, the message
	 *     may be preserved if it was not repeatable.
	 * </p>
	 * @return A new Message object that is a copy of this Message.
	 * @throws IOException If an I/O error occurs during the copying process.
	 */
	@Nonnull
	public Message copyMessage() throws IOException {
		final Message newMessage;
		if (!isRepeatable()) {
			preserve();
		}
		if (request instanceof SerializableFileReference) {
			final SerializableFileReference newRef;
			if (isBinary()) {
				newRef = SerializableFileReference.of(asInputStream());
			} else {
				newRef = SerializableFileReference.of(asReader(), getCharset());
			}
			newMessage = Message.asMessage(newRef);
		} else {
			newMessage = Message.asMessage(request);
		}
		newMessage.context = copyContext();
		return newMessage;
	}
}
