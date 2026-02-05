/*
   Copyright 2022-2026 WeAreFrank!

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
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.LinkedCaseInsensitiveMap;
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
	public static final String IS_ERROR_MESSAGE = "IsErrorMessage";

	private static final Logger LOG = LogManager.getLogger(MessageContext.class);

	@Serial
	private static final long serialVersionUID = 1L;
	private static final long CUSTOM_SERIALIZATION_VERSION = 1L;

	private Map<String, Serializable> data = new LinkedCaseInsensitiveMap<>();

	public MessageContext() {
		super();
	}
	public MessageContext(@Nullable String charset) {
		this();
		withCharset(charset);
	}
	public MessageContext(Map<String, Serializable> base) {
		this();
		withAllFrom(base);
	}

	public MessageContext(@NonNull MessageContext base) {
		this(base.data);
	}

	public MessageContext withAllFrom(@Nullable Map<String, Serializable> base) {
		if (base != null) {
			putAll(base);
		}
		return this;
	}

	public void putAll(@Nullable Map<String, Serializable> base) {
		if (base != null) {
			base.forEach(this::put);
		}
	}

	/**
	 * Put key in the message context. If the key already exists, it is overwritten with this value. If the value is NULL then the key is removed.
	 */
	public void put(@NonNull String key, @Nullable Serializable value) {
		if (value != null) {
			data.put(key, value);
		} else {
			data.remove(key);
		}
	}

	@Nullable Serializable remove(@NonNull String key) {
		return data.remove(key);
	}

	public @Nullable Serializable get(@NonNull String key) {
		return data.get(key);
	}

	public Map<String, Serializable> getAll() {
		return Collections.unmodifiableMap(data);
	}

	public boolean containsKey(@NonNull String key) {
		return data.containsKey(key);
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}

	public Set<Map.Entry<String, Serializable>> entrySet() {
		return data.entrySet();
	}

	/**
	 * Adds supplied charset to the message context.
	 * @param charset to add
	 * @return MessageContext with charset added
	 */
	public MessageContext withCharset(@Nullable String charset) {
		if (StringUtils.isNotEmpty(charset)) {
			put(METADATA_CHARSET, charset);
		}
		return this;
	}
	public MessageContext withCharset(@Nullable Charset charset) {
		if (charset!=null) {
			put(METADATA_CHARSET, charset.name());
		}
		return this;
	}
	public MessageContext withMimeType(@NonNull String mimeType) {
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

	public MessageContext withMimeType(@NonNull MimeType mimeType) {
		if (MediaType.APPLICATION_JSON.equalsTypeAndSubtype(mimeType) && mimeType.getCharset() != null) {
			// Strip the charset when JSON, see: https://www.rfc-editor.org/rfc/rfc8259
			remove(METADATA_CHARSET);
			Map<String, String> params = new HashMap<>(mimeType.getParameters());
			params.remove("charset");
			return withMimeType(new MimeType(mimeType, params));
		}

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
	public MessageContext withModificationTime(@Nullable Date time) {
		if (time != null) {
			put(METADATA_MODIFICATIONTIME, DateFormatUtils.format(time));
		}
		return this;
	}
	public MessageContext withModificationTime(@Nullable Instant time) {
		if (time != null) {
			put(METADATA_MODIFICATIONTIME, DateFormatUtils.format(time));
		}
		return this;
	}
	public MessageContext withName(@Nullable String name) {
		put(METADATA_NAME, name);
		return this;
	}
	public MessageContext withLocation(@Nullable String location) {
		put(METADATA_LOCATION, location);
		return this;
	}
	public MessageContext with(@NonNull String name, @Nullable Serializable value) {
		put(name, value);
		return this;
	}

	@Serial
	private void writeObject(@NonNull ObjectOutputStream out) throws IOException {
		// If in future we need to make incompatible changes we can keep reading old version by selecting on version-nr
		out.writeLong(CUSTOM_SERIALIZATION_VERSION);
		out.writeObject(data);
	}

	@Serial
	@SuppressWarnings("unchecked")
	private void readObject(@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.readLong(); // Custom serialization version; only version 1 yet so value can be ignored for now.
		data = (Map<String, Serializable>) in.readObject();
	}
}
