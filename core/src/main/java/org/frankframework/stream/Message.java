/*
   Copyright 2019-2025 WeAreFrank!

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

import java.io.BufferedInputStream;
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
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Lombok;

import org.frankframework.core.PipeLineSession;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CleanerProvider;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;

/**
 * A {@link Serializable} wrapper around data passed to the Frank!Framework and between pipes in the
 * pipeline.
 * <p>
 *     Regardless of the original format of the data, the system will always allow repeatable access to the data
 *     in multiple formats, such as {@code InputStream}, {@code Reader}, {@code byte[]}, {@code String} and
 *     for XML data, also as {@code InputSource} or {@code Source}.
 * </p>
 * <p>
 *     The Frank!Framework will intelligently buffer message data to memory or disk depending on size and
 *     configured limits. The limit for data held in memory is controlled via property {@value MESSAGE_MAX_IN_MEMORY_PROPERTY}. The default
 *     value is {@value MESSAGE_MAX_IN_MEMORY_DEFAULT}.
 * </p>
 * <p>
 *     Operations on a Message that change state, such as {@link #preserve(boolean)} (and implicitly {@link #copyMessage()}) are
 *     not thread-safe. If there is a chance that these operations are simultanuously executed from multiple threads, they need to be
 *     wrapped in a {@code synchronized} block that synchronizes on the message instance.
 * </p>
 */
public class Message implements Serializable, Closeable {
	public static final long MESSAGE_SIZE_UNKNOWN = -1L;
	public static final long MESSAGE_MAX_IN_MEMORY_DEFAULT = 5120L * 1024L;
	public static final String MESSAGE_MAX_IN_MEMORY_PROPERTY = "message.max.memory.size";

	private static final Logger LOG = LogManager.getLogger(Message.class);

	private static final @Serial long serialVersionUID = 437863352486501445L;
	private transient MessageNotClosedAction messageNotClosedAction;

	private @Nullable Object request;
	private @Getter @Nonnull String requestClass;

	private @Getter @Nonnull MessageContext context;
	private boolean failedToDetermineCharset = false;

	private @Getter boolean closed = false;

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

		// Cache size before possibly replacing FileInputStream or other streams of which the size is knowable with a wrapper
		if (!context.containsKey(MessageContext.METADATA_SIZE)) {
			long cacheSize = size();
			if (cacheSize != MESSAGE_SIZE_UNKNOWN) {
				context.put(MessageContext.METADATA_SIZE, cacheSize);
			}
		}

		if (this.request instanceof InputStream source) {
			this.request = new RepeatableInputStreamWrapper(source);
		} else if (this.request instanceof Reader source) {
			this.request = new RepeatableReaderWrapper(source);
		}

		if (request != null) {
			messageNotClosedAction = new MessageNotClosedAction();
			CleanerProvider.register(this, messageNotClosedAction);
		} else {
			messageNotClosedAction = null;
		}
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
	 * @param request      Request as {@link SerializableFileReference}
	 * @param context      {@link MessageContext}
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

	private static class MessageNotClosedAction implements Runnable {
		@Override
		public void run() {
			// No-op for now
		}
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
	 * If no Charset was provided when the Message object was created and
	 * the requested Charset is <code>auto</auto>, try to parse the Charset using
	 * {@link MessageUtils#computeDecodingCharset(Message)}.
	 * 
	 * If unsuccessful return the default Charset: {@link StreamUtil#DEFAULT_INPUT_STREAM_ENCODING UTF_8}.
	 * 
	 * @param defaultDecodingCharset The 'I know better' Charset, only used when no Charset is provided when the Message was created.
	 */
	@Nonnull
	protected String computeDecodingCharset(String defaultDecodingCharset) throws IOException {
		String providedCharset = getCharset();

		if (StringUtils.isEmpty(providedCharset)) {
			providedCharset = StringUtils.isNotEmpty(defaultDecodingCharset) ? defaultDecodingCharset : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		if (StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(providedCharset)) {
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

		return providedCharset;
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
	 * <p>
	 *     This operation can potentially modify the contents of the Message object.
	 * </p>
	 *
	 * @throws IOException Throws IOException if the Message cannot be read or writing fails.
	 */
	private void preserve(boolean deepPreserve) throws IOException {
		if (request == null) {
			return;
		}
		if (request instanceof SerializableFileReference) {
			return;
		}
		if (request instanceof RequestBuffer requestBuffer) {
			// RequestBuffer knows how to preserve itself, intelligently deciding to preserve to memory or disk
			request = requestBuffer.asSerializable();
			requestBuffer.close();
			return;
		}

		long requestSize = size();
		long maxInMemory = AppConstants.getInstance().getLong(MESSAGE_MAX_IN_MEMORY_PROPERTY, MESSAGE_MAX_IN_MEMORY_DEFAULT);
		if (requestSize == MESSAGE_SIZE_UNKNOWN || requestSize > maxInMemory) {
			preserveToDisk(deepPreserve);
			// Check again the size now that we know it for sure. If it fits into memory, better for performance to keep it in memory!
			if (requestSize == MESSAGE_SIZE_UNKNOWN && size() <= maxInMemory && request instanceof SerializableFileReference serializableFileReference) {
				if (isBinary()) {
					try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
						InputStream inputStream = serializableFileReference.getInputStream()) {
						inputStream.transferTo(bos);
						this.request = bos.toByteArray();
					}
				} else {
					try (StringWriter sw = new StringWriter();
						 Reader reader = serializableFileReference.getReader()) {
						reader.transferTo(sw);
						this.request = sw.toString();
					}
				}
				serializableFileReference.close();
			}
		} else {
			preserveToMemory(deepPreserve);
		}
	}

	private void preserveToMemory(boolean deepPreserve) throws IOException {
		if (request instanceof SerializableFileReference) {
			// Should not happen but just in case.
			return;
		}
		if (request instanceof Reader reader) {
			LOG.debug("preserving Reader {} as String", this::getObjectId);
			request = StreamUtil.readerToString(reader, null);
			return;
		}
		if (request instanceof InputStream stream) {
			LOG.debug("preserving InputStream {} as byte[]", this::getObjectId);
			request = StreamUtil.streamToBytes(stream);
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
		if (request instanceof String string) {
			request = SerializableFileReference.of(string, computeDecodingCharset(null));
		} else if (request instanceof byte[] bytes) {
			request = SerializableFileReference.of(bytes);
		} else if (deepPreserve) {
			if (isBinary()) {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				request = SerializableFileReference.of(asInputStream());
			} else {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				request = SerializableFileReference.of(asReader(), computeDecodingCharset(null));
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
		if (request instanceof SerializableFileReference reference) {
			return reference.isBinary();
		}

		if (request instanceof RequestBuffer requestBuffer) {
			return requestBuffer.isBinary();
		}

		return request instanceof ThrowingSupplier || request instanceof byte[] || request instanceof Number || request instanceof Boolean;
	}

	/**
	 * If true, the Message should preferably be read using a streaming method, i.e. asReader() or asInputStream(), to avoid copying it into memory.
	 */
	public boolean requiresStream() {
		return request instanceof ThrowingSupplier || request instanceof SerializableFileReference || request instanceof RequestBuffer;
	}

	@Override
	public void close() {
		if (request instanceof AutoCloseable closeable) {
			CloseUtils.closeSilently(closeable);
		}
		request = null;
		closed = true;
		CleanerProvider.clean(messageNotClosedAction);
	}

	public void closeOnCloseOf(@Nonnull PipeLineSession session) {
		if (this.request == null || isScheduledForCloseOnExitOf(session)) {
			return;
		}
		LOG.debug("registering Message [{}] for close on exit", this);
		session.scheduleCloseOnSessionExit(this);
	}

	public boolean isScheduledForCloseOnExitOf(@Nonnull PipeLineSession session) {
		return session.isScheduledForCloseOnExit(this);
	}

	public void unscheduleFromCloseOnExitOf(@Nonnull PipeLineSession session) {
		session.unscheduleCloseOnSessionExit(this);
		if (request instanceof AutoCloseable closeable) {
			session.unscheduleCloseOnSessionExit(closeable);
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
	 */
	@Nullable
	public Reader asReader() throws IOException {
		return asReader(null);
	}

	/**
	 * Return a {@link Reader} backed by the data in this message. {@link Reader#markSupported()} is guaranteed to be true for the returned stream.
	 *
	 * @param defaultDecodingCharset is only used when {@link #isBinary()} is {@code true}.
	 */
	@Nullable
	public Reader asReader(@Nullable String defaultDecodingCharset) throws IOException {
		if (request == null) {
			return null;
		}

		if (request instanceof SerializableFileReference reference && !reference.isBinary()) {
			LOG.debug("returning SerializableFileReference {} as Reader", this::getObjectId);
			// The Message was saved with a Charset (see PreserveToDisk), so read it with the Charset
			return reference.getReader();
		}

		if (request instanceof RequestBuffer requestBuffer) {
			String readerCharset = computeDecodingCharset(defaultDecodingCharset); // Don't overwrite the Message's charset unless it's set to AUTO
			return requestBuffer.asReader(Charset.forName(readerCharset));
		}

		if (isBinary()) {
			String readerCharset = computeDecodingCharset(defaultDecodingCharset); // Don't overwrite the Message's charset unless it's set to AUTO

			LOG.debug("returning InputStream {} as Reader", this::getObjectId);
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
			LOG.debug("returning Node {} as Reader", this::getObjectId);
			return new StringReader(Objects.requireNonNull(asString()));
		}
		LOG.debug("returning String {} as Reader", this::getObjectId);
		return new StringReader(request.toString());
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 */
	@Nullable
	public InputStream asInputStream() throws IOException {
		return asInputStream(null);
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 *
	 * @param defaultEncodingCharset is only used when the Message object is of character type (String)
	 */
	@Nullable
	public InputStream asInputStream(@Nullable String defaultEncodingCharset) throws IOException {
		try {
			if (request == null) {
				return null;
			}

			if (request instanceof RequestBuffer requestBuffer) {
				LOG.debug("returning InputStream {} from RequestBuffer", this::getObjectId);
				if (requestBuffer.isBinary()) {
					return requestBuffer.asInputStream();
				}
				String charset = getEncodingCharset(defaultEncodingCharset);
				return requestBuffer.asInputStream(Charset.forName(charset));
			}

			if (request instanceof SerializableFileReference reference) {
				LOG.debug("returning InputStream {} from SerializableFileReference", this::getObjectId);
				return reference.getInputStream();
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
			if (request instanceof byte[] bytes) {
				LOG.debug("returning byte[] {} as InputStream", this::getObjectId);
				return new ByteArrayInputStream(bytes);
			}
			if (request instanceof Node) {
				LOG.debug("returning Node {} as InputStream", this::getObjectId);
				return new ByteArrayInputStream(asByteArray());
			}
			String charset = getEncodingCharset(defaultEncodingCharset);
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
		try (Reader r = asReader()) {
			if (r == null) {
				return "";
			}
			char[] buffer = new char[readLimit];
			int len = r.read(buffer);
			if (len <= 0) {
				return "";
			}
			return new String(buffer, 0, len);
		}
	}

	/**
	 * return the request object as a {@link InputSource}.
	 */
	@Nullable
	public InputSource asInputSource() throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof InputSource source) {
			LOG.debug("returning InputSource {} as InputSource", this::getObjectId);
			return source;
		}
		if (request instanceof Reader reader) {
			LOG.debug("returning Reader {} as InputSource", this::getObjectId);
			return new InputSource(reader);
		}
		if (request instanceof String string) {
			LOG.debug("returning String {} as InputSource", this::getObjectId);
			return new InputSource(new StringReader(string));
		}
		LOG.debug("returning {} as InputSource", this::getObjectId);
		if (isBinary() && getCharset() == null) { // When a charset is present it should be used.
			return new InputSource(asInputStream());
		}
		return new InputSource(asReader());
	}

	/**
	 * return the request object as a {@link Source}.
	 */
	@Nullable
	public Source asSource() throws IOException, SAXException {
		if (request == null) {
			return null;
		}
		if (request instanceof Source source) {
			LOG.debug("returning Source {} as Source", this::getObjectId);
			return source;
		}
		if (request instanceof Node node) {
			LOG.debug("returning Node {} as DOMSource", this::getObjectId);
			return new DOMSource(node);
		}
		LOG.debug("returning {} as Source", this::getObjectId);
		return XmlUtils.inputSourceToSAXSource(asInputSource());
	}

	/**
	 * Return the request object as a byte array. This may have the side effect of preserving the input as byte array.
	 * This operation is not thread-safe.
	 */
	@Nullable
	public byte[] asByteArray() throws IOException {
		return asByteArray(null);
	}

	/**
	 * Return the request object as a byte array. This may have the side effect of preserving the input as byte array.
	 * This operation is not thread-safe.
	 */
	@Nullable
	public byte[] asByteArray(@Nullable String defaultEncodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof byte[] bytes) {
			return bytes;
		}
		if (request instanceof Node node) {
			try {
				LOG.debug("returning Node {} as byte[]", this::getObjectId);
				return XmlUtils.nodeToByteArray(node);
			} catch (TransformerException e) {
				throw new IOException("Could not convert Node " + getObjectId() + " to byte[]", e);
			}
		}
		String charset = getEncodingCharset(defaultEncodingCharset);
		if (request instanceof String string) {
			return string.getBytes(charset);
		}
		if (request instanceof ThrowingSupplier || request instanceof SerializableFileReference) {
			LOG.debug("returning InputStream {} from supplier", this::getObjectId);
			return StreamUtil.streamToBytes(asInputStream());
		}
		// save the generated byte array as the request before returning it, unless it's too big
		byte[] result = StreamUtil.streamToBytes(asInputStream(charset));
		if (result.length < AppConstants.getInstance().getInt(MESSAGE_MAX_IN_MEMORY_PROPERTY, (int) MESSAGE_MAX_IN_MEMORY_DEFAULT)) {
			request = result;
		}
		return result;
	}

	/**
	 * return the request object as a String. This may have the side effect of preserving the input as a String, thus
	 * modifying the state of the Message object.
	 * This operation is not thread-safe.
	 */
	@Nullable
	public String asString() throws IOException {
		return asString(null);
	}

	/**
	 * return the request object as a String. This may have the side effect of preserving the input as a String, thus
	 * modifying the state of the Message object.
	 * This operation is not thread-safe.
	 */
	@Nullable
	public String asString(@Nullable String decodingCharset) throws IOException {
		if (request == null) {
			return null;
		}
		if (request instanceof String string) {
			return string;
		}
		if (request instanceof Number || request instanceof Boolean) {
			return request.toString();
		}
		if (request instanceof Node node) {
			try {
				LOG.debug("returning Node {} as String", this::getObjectId);
				return XmlUtils.nodeToString(node);
			} catch (TransformerException e) {
				throw new IOException("Could not convert type Node " + getObjectId() + " to String", e);
			}
		}

		// save the generated String as the request before returning it
		// Specify initial capacity a little larger than file-size just as extra safeguard we do not re-allocate buffer.
		String result = StreamUtil.readerToString(asReader(decodingCharset), null, false, (int) size() + 32);
		if (!(request instanceof SerializableFileReference) && !isBinary() && result.length() < AppConstants.getInstance().getInt(MESSAGE_MAX_IN_MEMORY_PROPERTY, (int) MESSAGE_MAX_IN_MEMORY_DEFAULT)) {
			if (request instanceof AutoCloseable closeable) {
				try {
					closeable.close();
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

	/** @return true if the request is or extends of the specified type at parameter clazz */
	public boolean isRequestOfType(Class<?> clazz) {
		if (request == null) {
			return false;
		}
		return clazz.equals(request.getClass()) || clazz.isAssignableFrom(request.getClass());
	}

	/**
	 * Check if a message is empty. If message size cannot be determined, return {@code false} to be on the safe side although this
	 * might not be strictly correct.
	 *
	 * @return {@code true} if the message is empty, {@code false} if message is not empty or if the size cannot be determined up-front.
	 */
	public boolean isEmpty() {
		if (request instanceof RequestBuffer requestBuffer) {
			try {
				return requestBuffer.isEmpty();
			} catch (IOException e) {
				throw Lombok.sneakyThrow(e);
			}
		}
		return size() == 0L;
	}

	private void toStringPrefix(StringBuilder writer) {
		if (context.isEmpty() || !LOG.isDebugEnabled()) {
			return;
		}
		writer.append("context:\n");
		for (Entry<String, Serializable> entry : context.entrySet()) {
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
		var result = new StringBuilder();

		toStringPrefix(result);
		result.append(getObjectId());

		if (request != null) {
			result.append(": [").append(request).append("]");
		}


		return result.toString();
	}

	/**
	 * Returns the message identifier and which resource class it represents
	 *
	 * @return Message[1234abcd:ByteArrayInputStream]
	 */
	public String getObjectId() {
		return ClassUtils.classNameOf(this) + "[" + Integer.toHexString(hashCode()) + ":" + getRequestClass() + "]";
	}

	/**
	 * Please note that this method should only be used when you don't know the type of object. In all other cases,
	 * use the constructor of {@link Message} or a more applicable subclass like {@link FileMessage} or {@link UrlMessage}
	 *
	 * @return a Message of the correct type for the given object
	 */
	public static Message asMessage(Object object) {
		if (object == null) {
			return nullMessage();
		}
		if (object instanceof Message message) {
			// NB: This case can lead to hard-to-debug issues with messages either not being closed, or closed too early. Should ideally be avoided.
			message.assertNotClosed();
			return message;
		}
		if (object instanceof URL rL) {
			return new UrlMessage(rL);
		}
		if (object instanceof File file) {
			return new FileMessage(file);
		}
		if (object instanceof Path path) {
			return new PathMessage(path);
		}
		if (object instanceof RawMessageWrapper) {
			throw new IllegalArgumentException("Raw message extraction / wrapping should be done via Listener.");
		}
		return new Message(new MessageContext(), object);
	}

	/**
	 * Check if the message passed is null or empty.
	 *
	 * @param message Message to check. Can be {@code null}.
	 * @return Returns {@code true} if the message is {@code null}, otherwise the result of {@link Message#isEmpty()}.
	 */
	public static boolean isEmpty(Message message) {
		return message == null || message.isEmpty();
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
	 */
	public static boolean hasDataAvailable(Message message) throws IOException {
		if (Message.isNull(message)) {
			return false;
		}
		// TODO: Rewrite "message.isEmpty()" to give a truthful answer always? Then we can delegate to that instead. Don't yet know if that will break some other tests though, so this will be a future change.
		if (message.asObject() instanceof RequestBuffer requestBuffer) {
			return !requestBuffer.isEmpty();
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
		try (r) {
			return r.read() != -1;
		} catch (EOFException e) {
			return false;
		}
	}

	private static boolean checkIfStreamHasData(InputStream is) throws IOException {
		if (is == null) {
			return false;
		}
		try (is){
			return is.read() != -1;
		} catch (EOFException e) {
			return false;
		}
	}

	public static boolean isNull(@Nullable Message message) {
		return message == null || message.isNull();
	}

	/*
	 * this method is used by Serializable, to serialize objects to a stream.
	 */
	@Serial
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
	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		var charset = (String) stream.readObject();
		request = stream.readObject();
		try {
			Object requestClassFromStream = stream.readObject();
			if (requestClassFromStream != null) {
				if (requestClassFromStream instanceof Class<?> class1) {
					this.requestClass = class1.getTypeName();
				} else {
					this.requestClass = requestClassFromStream.toString();
				}
			} else {
				this.requestClass = ClassUtils.nameOf(request);
			}
		} catch (Exception e) {
			requestClass = ClassUtils.nameOf(request);
			LOG.warn("Could not read requestClass, using ClassUtils.nameOf(request) [{}], ({}): {}", () -> requestClass, () -> ClassUtils.nameOf(e), e::getMessage);
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
		// Register the message for cleaning later
		messageNotClosedAction = new MessageNotClosedAction();
		CleanerProvider.register(this, messageNotClosedAction);
	}

	public void assertNotClosed() {
		if (isClosed()) {
			throw new IllegalStateException(getObjectId() + " is used after is has been closed!");
		}
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

		if (request instanceof String || request instanceof Number || request instanceof Boolean) {
			long size = request.toString().getBytes(StreamUtil.DEFAULT_CHARSET).length;
			getContext().put(MessageContext.METADATA_SIZE, size);
			return size;
		}

		if (request instanceof byte[] bytes) {
			return bytes.length;
		}

		if (request instanceof SerializableFileReference reference) {
			return reference.getSize();
		}

		if (request instanceof RequestBuffer requestBuffer) {
			return requestBuffer.size();
		}

		if (request instanceof FileInputStream fileStream) {
			// This can happen during initial check of request-size before creating the RequestBuffer
			try {
				return fileStream.getChannel().size();
			} catch (IOException e) {
				LOG.debug("unable to determine size of stream [{}], error: {}", (Supplier<?>) () -> ClassUtils.nameOf(request), (Supplier<?>) e::getMessage, e);
			}
		}

		if (!(request instanceof InputStream || request instanceof Reader)) {
			//Unable to determine the size of a Stream
			LOG.debug("unable to determine size of Message [{}]", () -> ClassUtils.nameOf(request));
		}

		return MESSAGE_SIZE_UNKNOWN;
	}

	/**
	 * Creates a copy of this Message object.
	 * <p>
	 * <b>NB:</b> To copy the underlying value of the message object, the message
	 * may be preserved if it was not repeatable. Thus this operation may modify the
	 * state of the message object.
	 * </p>
	 *
	 * @return A new Message object that is a copy of this Message.
	 * @throws IOException If an I/O error occurs during the copying process.
	 */
	@Nonnull
	public Message copyMessage() throws IOException {
		if (request instanceof RequestBuffer) {
			preserve(false);
		}
		if (!(request instanceof SerializableFileReference)) {
			return new Message(copyContext(), request);
		}
		final SerializableFileReference newRef;
		if (isBinary()) {
			newRef = SerializableFileReference.of(asInputStream());
		} else {
			newRef = SerializableFileReference.of(asReader(), getCharset());
		}
		return new Message(copyContext(), newRef);
	}

	public static boolean isFormattedErrorMessage(Message message) {
		return (message != null && Boolean.TRUE.equals(message.getContext().get(MessageContext.IS_ERROR_MESSAGE)));
	}
}
