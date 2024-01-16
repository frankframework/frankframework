/*
   Copyright 2022-2023 WeAreFrank!

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

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

public class MessageContext extends LinkedHashMap<String,Object> {
	private static final Logger LOG = LogManager.getLogger(MessageContext.class);

	public static final String HEADER_PREFIX = "Header.";

	public static final String METADATA_CHARSET = "Metadata.Charset";
	public static final String METADATA_SIZE = "Metadata.Size";
	public static final String METADATA_MODIFICATIONTIME = "Metadata.ModificationTime";
	public static final String METADATA_NAME = "Metadata.Name";
	public static final String METADATA_LOCATION = "Metadata.Location";
	public static final String METADATA_MIMETYPE = "Metadata.MimeType";

	public MessageContext() {
		super();
	}
	public MessageContext(String charset) {
		this();
		withCharset(charset);
	}
	public MessageContext(Map<String,Object> base) {
		this();
		withAllFrom(base);
	}

	public MessageContext withAllFrom(Map<String,Object> base) {
		if (base!=null) {
			putAll(base);
		}
		return this;
	}
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
}
