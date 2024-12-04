/*
   Copyright 2022-2024 WeAreFrank!

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

public class MessageContext implements Serializable {
	public static final String CONTEXT_PIPELINE_CALLER = "Pipeline.Caller";
	public static final String CONTEXT_PREVIOUS_PIPE = "Pipeline.PreviousPipe";

	public static final String HEADER_PREFIX = "Header.";

	public static final String METADATA_CHARSET = "Metadata.Charset";
	public static final String METADATA_SIZE = "Metadata.Size";
	public static final String METADATA_MODIFICATIONTIME = "Metadata.ModificationTime";
	public static final String METADATA_NAME = "Metadata.Name";
	public static final String METADATA_LOCATION = "Metadata.Location";
	public static final String METADATA_MIMETYPE = "Metadata.MimeType";

	private static final Logger LOG = LogManager.getLogger(MessageContext.class);

	private static final long serialVersionUID = 1L;
	private static final long customSerializationVersion = 1L;

	private Map<String, Object> data = new LinkedHashMap<>();

	public MessageContext() {
		super();
	}
	public MessageContext(String charset) {
		this();
		withCharset(charset);
	}
	public MessageContext(Map<String, ?> base) {
		this();
		withAllFrom(base);
	}

	public MessageContext(MessageContext base) {
		this(base.data);
	}

	public MessageContext withAllFrom(Map<String,?> base) {
		if (base!=null) {
			putAll(base);
		}
		return this;
	}

	public void putAll(Map<String, ?> base) {
		if (base!=null) {
			data.putAll(base);
		}
	}

	public void put(String key, Object value) {
		data.put(key, value);
	}

	Object remove(String key) {
		return data.remove(key);
	}

	public Object get(String key) {
		return data.get(key);
	}

	public boolean containsKey(String key) {
		return data.containsKey(key);
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		return data.entrySet();
	}

	/**
	 * Adds supplied charset to the message context.
	 * @param charset to add
	 * @return MessageContext with charset added
	 */
	public MessageContext withCharset(String charset) {
		if (StringUtils.isNotEmpty(charset)) {
			put(METADATA_CHARSET, charset);
		}
		return this;
	}
	public MessageContext withCharset(Charset charset) {
		if (charset!=null) {
			put(METADATA_CHARSET, charset.name());
		}
		return this;
	}
	public MessageContext withMimeType(String mimeType) {
		try {
			withMimeType(MimeType.valueOf(mimeType));
		} catch (InvalidMimeTypeException imte) {
			String parsed = StringUtil.splitToStream(mimeType, ";").filter(e -> !e.contains("=")).findFirst().orElse(null);
			if(parsed != null) {
				try {
					return withMimeType(MimeType.valueOf(parsed));
				} catch (InvalidMimeTypeException imte2) {
					LOG.debug("tried to parse cleansed mimetype [{}]", parsed, imte2);
				}
			}
			LOG.warn("unable to parse mimetype from string [{}]", mimeType, imte);
		}

		return this;
	}

	public MessageContext withMimeType(MimeType mimeType) {
		put(METADATA_MIMETYPE, mimeType);
		withCharset(mimeType.getCharset());

		return this;
	}

	public MimeType getMimeType() {
		return (MimeType) get(METADATA_MIMETYPE);
	}
	public MessageContext withSize(long size) {
		if (size >= 0) {
			put(METADATA_SIZE, size);
		}
		return this;
	}
	public MessageContext withoutSize() {
		remove(METADATA_SIZE);
		return this;
	}
	public MessageContext withModificationTime(long time) {
		return withModificationTime(Instant.ofEpochMilli(time));
	}
	public MessageContext withModificationTime(Date time) {
		if (time!=null) {
			put(METADATA_MODIFICATIONTIME, DateFormatUtils.format(time));
		}
		return this;
	}
	public MessageContext withModificationTime(Instant time) {
		if (time!=null) {
			put(METADATA_MODIFICATIONTIME, DateFormatUtils.format(time));
		}
		return this;
	}
	public MessageContext withName(String name) {
		put(METADATA_NAME, name);
		return this;
	}
	public MessageContext withLocation(String location) {
		put(METADATA_LOCATION, location);
		return this;
	}
	public MessageContext with(String name, String value) {
		put(name, value);
		return this;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		// If in future we need to make incompatible changes we can keep reading old version by selecting on version-nr
		out.writeLong(customSerializationVersion);
		Map<String, Serializable> serializableData = data.entrySet().stream()
						.filter(e -> {
							if (e.getValue() instanceof Serializable) return true;
							else {
								LOG.warn("Cannot write non-serializable MessageContext entry to stream: [{}] -> [{}]", e::getKey, e::getValue);
								return false;
							}
						})
						.collect(Collectors.toMap(Map.Entry::getKey, e -> (Serializable)(e.getValue())));
		out.writeObject(serializableData);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.readLong(); // Custom serialization version; only version 1 yet so value can be ignored for now.
		//noinspection unchecked
		data = (Map<String, Object>) in.readObject();
	}
}
