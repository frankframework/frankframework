/*
   Copyright 2019-2026 WeAreFrank!

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map.Entry;

import javax.xml.transform.Source;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.SneakyThrows;

import org.frankframework.dataconversion.DataConverter;
import org.frankframework.dataconversion.DataConverterFactory;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

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
 *     Operations on a Message that change state, such as {@link #preserve(boolean)} (and implicitly serializing the Message) are
 *     not thread-safe. If there is a chance that these operations are simultaneously executed from multiple threads, they need to be
 *     wrapped in a {@code synchronized} block that synchronizes on the message instance.
 * </p>
 */
@NullMarked
public class Message implements Serializable {
	public static final long MESSAGE_SIZE_UNKNOWN = -1L;
	public static final long MESSAGE_MAX_IN_MEMORY_DEFAULT = 5120L * 1024L;
	public static final String MESSAGE_MAX_IN_MEMORY_PROPERTY = "message.max.memory.size";
	public static final long MESSAGE_MAX_IN_MEMORY = AppConstants.getInstance().getLong(MESSAGE_MAX_IN_MEMORY_PROPERTY, MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private static final Logger LOG = LogManager.getLogger(Message.class);

	private static final @Serial long serialVersionUID = 437863352486501445L;

	private transient DataConverter dataConverter;
	private @Getter String requestClass;

	private @Getter MessageContext context;
	private boolean failedToDetermineCharset = false;

	private Message(final MessageContext context, final @Nullable Object request, final @Nullable Class<?> requestClass) {
		this.dataConverter = DataConverterFactory.getConverter(request, this::computeCharsetOrNull);
		this.context = context;
		this.requestClass = requestClass != null ? ClassUtils.nameOf(requestClass) : ClassUtils.nameOf(request);
	}

	private Message(MessageContext context, @Nullable Object request) {
		this(context, request, request != null ? request.getClass() : null);
	}

	public Message(String request, MessageContext context) {
		this(context, request);
	}

	public Message(String request) {
		this(new MessageContext(), request);
	}

	public Message(byte[] request, String charset) {
		this(new MessageContext(charset), request);
	}

	public Message(byte[] request, MessageContext context) {
		this(context, request);
	}

	public Message(byte[] request) {
		this(new MessageContext(), request);
	}

	public Message(Reader request, MessageContext context) throws IOException {
		this.context = context;
		this.requestClass = ClassUtils.nameOf(request);
		Message temporaryMessage = MessageUtils.fromReader(request);
		copyFromTemporaryMessage(temporaryMessage);
		this.dataConverter = DataConverterFactory.getConverter(temporaryMessage.dataConverter.asRawObject(), this::computeCharsetOrNull);
		if (this.context.containsKey(MessageContext.METADATA_CHARSET)) {
			// Ensure charset is now always UTF-8 because that's what it is after converting from stream
			this.context.withCharset(StandardCharsets.UTF_8);
		}
	}

	public Message(Reader request) throws IOException {
		this(request, new MessageContext());
	}

	/**
	 * Constructor for Message using InputStream supplier. It is assumed the InputStream can be supplied multiple times.
	 */
	protected Message(ThrowingSupplier<InputStream, Exception> request, MessageContext context, Class<?> requestClass) {
		this(context, request, requestClass);
	}

	/**
	 * Constructor for Message using a {@link SerializableFileReference}.
	 *
	 * @param request      Request as {@link SerializableFileReference}
	 * @param context      {@link MessageContext}
	 * @param requestClass {@link Class} of the original request from which the {@link SerializableFileReference} request was created
	 */
	protected Message(SerializableFileReference request, MessageContext context, Class<?> requestClass) {
		this(context, request, requestClass);
	}

	public Message(InputStream request, String charset) throws IOException {
		this(request, new MessageContext(charset));
	}

	public Message(InputStream request, MessageContext context) throws IOException {
		this(request, context, request.getClass());
	}

	protected Message(InputStream request, MessageContext context, Class<?> requestClass) throws IOException {
		this.context = context;
		this.requestClass = ClassUtils.nameOf(requestClass);
		Message temporaryMessage = MessageUtils.fromInputStream(request);
		copyFromTemporaryMessage(temporaryMessage);
		this.dataConverter = DataConverterFactory.getConverter(temporaryMessage.dataConverter.asRawObject(), this::computeCharsetOrNull);
	}

	private void copyFromTemporaryMessage(Message temporaryMessage) {
		// Copy all keys except the name, so we do not overwrite the original name (if given) with a potential temporary-file name.
		temporaryMessage.context.getAll().keySet()
				.stream()
				.filter(key -> !key.equals(MessageContext.METADATA_NAME))
				.forEachOrdered(key -> this.context.put(key, temporaryMessage.context.get(key)));
	}

	public Message(InputStream request) throws IOException {
		this(request, new MessageContext());
	}

	public Message(Node request, MessageContext context) {
		this(context, request);
	}

	public Message(Node request) {
		this(new MessageContext(), request);
	}

	public static Message nullMessage() {
		return nullMessage(new MessageContext());
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
	@Nullable
	public String getCharset() {
		return (String) context.get(MessageContext.METADATA_CHARSET);
	}

	public void setCharset(@Nullable String charset) {
		context.withCharset(charset);
	}
	public void setCharset(@Nullable Charset charset) {
		context.withCharset(charset);
	}

	/**
	 * Get the charset if set. If the charset = 'AUTO' then compute the charset from stream.
	 */
	private @Nullable String computeCharsetOrNull() throws IOException {
		String providedCharset = getCharset();
		if (StringUtils.isEmpty(providedCharset)) {
			return null;
		}
		if (!StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(providedCharset)) {
			return providedCharset;
		}
		if (failedToDetermineCharset || isEmpty()) {
			return null;
		}
		Charset computedCharset = MessageUtils.computeDecodingCharset(this.dataConverter.asInputStream());
		failedToDetermineCharset = (computedCharset == null);

		// Remove the size, if present, when the charset changes!
		context.remove(MessageContext.METADATA_SIZE);
		setCharset(computedCharset);
		return computedCharset != null ? computedCharset.name() : null;
	}

	private String computeCharsetOrDefault() throws IOException {
		String charset = computeCharsetOrNull();
		if (StringUtils.isNotEmpty(charset)) {
			return charset;
		}
		setCharset(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		return StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	}

	/**
	 * If no Charset was provided when the Message object was created and
	 * the requested Charset is <code>auto</code>, try to parse the Charset using
	 * {@link MessageUtils#computeDecodingCharset(Message)}.
	 *
	 * If unsuccessful return the default Charset: {@link StreamUtil#DEFAULT_INPUT_STREAM_ENCODING UTF_8}.
	 *
	 * TODO: I'm sure we still need this method somewhere, and not only in tests?
	 * @param defaultDecodingCharset The 'I know better' Charset, only used when no Charset is provided when the Message was created.
	 */
	protected String computeDecodingCharset(@Nullable String defaultDecodingCharset) throws IOException {
		String providedCharset = getCharset();

		if (StringUtils.isEmpty(providedCharset)) {
			providedCharset = StringUtils.isNotEmpty(defaultDecodingCharset) ? defaultDecodingCharset : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}

		if (StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(providedCharset)) {
			Charset computedCharset = null;
			if (!failedToDetermineCharset && !isEmpty()) {
				computedCharset = MessageUtils.computeDecodingCharset(this.dataConverter.asInputStream());
			}

			// Remove the size, if present, when the charset changes!
			context.remove(MessageContext.METADATA_SIZE);

			if (computedCharset == null) {
				failedToDetermineCharset = true;
				if (StringUtils.isNotEmpty(defaultDecodingCharset) && !StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(defaultDecodingCharset)) {
					setCharset(defaultDecodingCharset);
					return defaultDecodingCharset;
				}
				setCharset(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
				return StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
			}
			setCharset(computedCharset);
			return computedCharset.name();
		}

		return providedCharset;
	}

	/**
	 * Notify the message object that the request object will be used multiple times.
	 * If the request object can only be read one time, it can turn it into a less volatile representation.
	 * <p>
	 *     This operation can potentially modify the contents of the Message object. This is not thread-safe.
	 * </p>
	 *
	 * @throws IOException Throws IOException if the Message cannot be read or writing fails.
	 */
	private void preserve(boolean deepPreserve) throws IOException {
		if (isNull()) {
			return;
		}

		long requestSize = size();
		// TODO: Only place where we still check instanceof; figure out a cleaner way to do the check if we should preserve our data to disk, or not
		Object data = dataConverter.asRawObject();
		if ((requestSize == MESSAGE_SIZE_UNKNOWN || requestSize > MESSAGE_MAX_IN_MEMORY) && !(data instanceof SerializableFileReference || data instanceof Date || data instanceof TemporalAccessor || data instanceof Boolean || data instanceof Number)) {
			preserveToDisk(deepPreserve);
			// Check again the size now that we know it for sure. If it fits into memory, better for performance to keep it in memory!
			if (requestSize == MESSAGE_SIZE_UNKNOWN && size() <= MESSAGE_MAX_IN_MEMORY && dataConverter.asRawObject() instanceof SerializableFileReference serializableFileReference) {
				loadSerializableFileReferenceToMemory(serializableFileReference);
			}
		} else {
			preserveToMemory(deepPreserve);
		}
	}

	private void loadSerializableFileReferenceToMemory(SerializableFileReference serializableFileReference) throws IOException {
		Object data;
		if (isBinary()) {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				InputStream inputStream = serializableFileReference.getInputStream()) {
				inputStream.transferTo(bos);
				data = bos.toByteArray();
			}
		} else {
			try (StringWriter sw = new StringWriter(); Reader reader = serializableFileReference.getReader()) {
				reader.transferTo(sw);
				data = sw.toString();
			}
		}
		this.dataConverter = DataConverterFactory.getConverter(data, this::computeCharsetOrNull);
		serializableFileReference.close();
	}

	private void preserveToMemory(boolean deepPreserve) throws IOException {
		Object data = dataConverter.asRawObject();
		if (data instanceof Serializable && !(data instanceof SerializableFileReference)) {
			// Should not happen but just in case.
			return;
		}

		// if deepPreserve=true, File and URL are also preserved as byte array
		// otherwise we rely on that File and URL can be repeatedly read
		if (deepPreserve) {
			if (data instanceof ThrowingSupplier<?, ?>) {
				LOG.debug("deep preserving {} as byte[]", this::getObjectId);
				this.dataConverter = DataConverterFactory.getConverter(asByteArray(), this::computeCharsetOrNull);
			} else if (data instanceof SerializableFileReference serializableFileReference) {
				// Load smaller serializable files into memory for better performance.
				loadSerializableFileReferenceToMemory(serializableFileReference);
			}
		}
	}

	/**
	 * Preserve message to disk.
	 *
	 * @throws IOException Throws {@link IOException} if the Message cannot be read, or no temporary file can be written to.
	 */
	private void preserveToDisk(boolean deepPreserve) throws IOException {
		Object data = dataConverter.asRawObject();
		if (data instanceof SerializableFileReference) {
			// Should not happen but just in case.
			return;
		}
		if (data instanceof String string) {
			data = SerializableFileReference.of(string, computeCharsetOrDefault());
		} else if (data instanceof byte[] bytes) {
			data = SerializableFileReference.of(bytes);
		} else if (deepPreserve) {
			if (isBinary()) {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				data = SerializableFileReference.of(asInputStream());
			} else {
				LOG.debug("preserving {} as SerializableFileReference", this::getObjectId);
				data = SerializableFileReference.of(asReader(), computeCharsetOrDefault());
			}
		}
		this.dataConverter = DataConverterFactory.getConverter(data, this::computeCharsetOrNull);
	}

	/**
	 * @deprecated Please avoid the use of the raw object wherever possible and if you must, annotate why.
	 */
	@Deprecated(since = "8")
	@SuppressWarnings("java:S1133")
	@Nullable
	public Object asObject() {
		return dataConverter.asRawObject();
	}

	public boolean isBinary() {
		return !context.containsKey(MessageContext.METADATA_CHARSET) && dataConverter.isBinary();
	}

	/**
	 * If true, the Message should preferably be read using a streaming method, i.e. asReader() or asInputStream(), to avoid copying it into memory.
	 */
	public boolean requiresStream() {
		return dataConverter.prefersStreaming();
	}

	/**
	 * Return a {@link Reader} backed by the data in this message. {@link Reader#markSupported()} is guaranteed to be true for the returned stream.
	 */
	public Reader asReader() throws IOException {
		return dataConverter.asReader();
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 */
	public InputStream asInputStream() throws IOException {
		return dataConverter.asInputStream();
	}

	/**
	 * Return an {@link InputStream} backed by the data in this message. {@link InputStream#markSupported()} is guaranteed to be true for the returned stream.
	 *
	 * @param defaultEncodingCharset is only used when the Message object is of character type (String)
	 */
	public InputStream asInputStream(@Nullable String defaultEncodingCharset) throws IOException {
		if (StringUtils.isEmpty(defaultEncodingCharset)) {
			return dataConverter.asInputStream();
		}
		return dataConverter.asInputStream(defaultEncodingCharset);
	}

	public synchronized String peek(int readLimit) throws IOException {
		if (isEmpty()) {
			return "";
		}
		if (dataConverter.asRawObject() instanceof String string) {
			return StringUtils.truncate(string, readLimit);
		}
		try (Reader r = asReader()) {
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
		return dataConverter.asInputSource();
	}

	/**
	 * return the request object as a {@link Source}.
	 */
	@Nullable
	public Source asSource() throws IOException, SAXException {
		return dataConverter.asSource();
	}

	/**
	 * Return the request object as a byte array.
	 */
	public byte @Nullable[] asByteArray() throws IOException {
		return dataConverter.asByteArray();
	}

	/**
	 * Return the request object as a byte array with the specified character encoding.
	 */
	public byte @Nullable[] asByteArray(@Nullable String defaultEncodingCharset) throws IOException {
		if (isNull()) return null;
		return StreamUtil.streamToBytes(asInputStream(defaultEncodingCharset));
	}

	/**
	 * return the request object as a String. This may have the side effect of preserving the input as a String, thus
	 * modifying the state of the Message object.
	 * This operation is not thread-safe.
	 */
	@Nullable
	public String asString() throws IOException {
		return dataConverter.asString();
	}

	public boolean isNull() {
		return dataConverter.isNull();
	}

	/** @return true if the request is or extends of the specified type at parameter clazz */
	public boolean isRequestOfType(Class<?> clazz) {
		Object data = dataConverter.asRawObject();
		if (data == null) {
			return false;
		}
		return clazz.equals(data.getClass()) || clazz.isAssignableFrom(data.getClass());
	}

	/**
	 * Check if a message is empty. If message size cannot be determined, check if any data can be read from the message.
	 *
	 * @return {@code true} if the message is empty or no data can be read from it, {@code false} if the size if larger than 0 or data can be read from it.
	 */
	public boolean isEmpty() {
		return dataConverter.isEmpty();
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

		if (LOG.isDebugEnabled()) {
			if (dataConverter.asRawObject() != null) {
				result.append(" content [").append(dataConverter.asRawObject()).append("]");
			} else {
				result.append(" no-content");
			}
		}

		result.append(" size [").append(size()).append("]");

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
	@SneakyThrows(IOException.class)
	public static Message asMessage(@Nullable Object object) {
		return switch (object) {
			case null -> nullMessage();
			case Message message -> message;
			case Reader reader -> MessageUtils.fromReader(reader);
			case InputStream stream -> MessageUtils.fromInputStream(stream);
			case URL rL -> new UrlMessage(rL);
			case File file -> new FileMessage(file);
			case Path path -> new PathMessage(path);
			case RawMessageWrapper<?> ignored -> throw new IllegalArgumentException("Raw message extraction / wrapping should be done via Listener.");
			default -> // Constructor will reject unsupported types
					new Message(new MessageContext(), object);
		};
	}

	/**
	 * Check if the message passed is null or empty.
	 *
	 * @param message Message to check. Can be {@code null}.
	 * @return Returns {@code true} if the message is {@code null}, otherwise the result of {@link Message#isEmpty()}.
	 */
	public static boolean isEmpty(@Nullable Message message) {
		return message == null || message.isEmpty();
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
		Object data = dataConverter.asRawObject();
		if (data != null && !(data instanceof Serializable)) {
			throw new IllegalArgumentException("This message contains a non-serializable request-object of type " + data.getClass().getName());
		}

		stream.writeObject(getCharset());
		stream.writeObject(data);
		stream.writeObject(requestClass);
		stream.writeObject(context);
	}

	/*
	 * this method is used by Serializable, to deserialize objects from a stream.
	 */
	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		var charset = (String) stream.readObject();
		Object request = stream.readObject();
		try {
			Object requestClassFromStream = stream.readObject();
			if (requestClassFromStream != null) {
				if (requestClassFromStream instanceof Class<?> class1) {
					requestClass = class1.getTypeName();
				} else {
					requestClass = requestClassFromStream.toString();
				}
			} else {
				requestClass = ClassUtils.nameOf(request);
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
		dataConverter = DataConverterFactory.getConverter(request, this::computeCharsetOrNull);
	}

	/**
	 * @return Message size or -1 if it can't determine the size.
	 */
	public long size() {
		Long sz = (Long) context.get(MessageContext.METADATA_SIZE);
		if (sz != null) {
			return sz;
		}
		long size = dataConverter.size();
		if (size != MESSAGE_SIZE_UNKNOWN) {
			context.withSize(size);
		}
		return size;
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
	public Message copyMessage() throws IOException {
		Object request = dataConverter.asRawObject();
		if (!(request instanceof SerializableFileReference)) {
			return new Message(copyContext(), request);
		}
		final SerializableFileReference newRef;
		if (isBinary() || StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(getCharset())) {
			newRef = SerializableFileReference.of(asInputStream());
		} else {
			newRef = SerializableFileReference.of(asReader(), getCharset());
		}
		return new Message(copyContext(), newRef);
	}

	public static boolean isFormattedErrorMessage(@Nullable Message message) {
		return (message != null && Boolean.TRUE.equals(message.getContext().get(MessageContext.IS_ERROR_MESSAGE)));
	}
}
