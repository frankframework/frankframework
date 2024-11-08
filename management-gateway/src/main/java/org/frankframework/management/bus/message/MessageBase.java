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
package org.frankframework.management.bus.message;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

import org.frankframework.management.bus.BusMessageUtils;

public class MessageBase<T> implements Message<T> {
	public static final String STATUS_KEY = "status";
	public static final String MIMETYPE_KEY = "type";
	public static final String CONTENT_DISPOSITION_KEY = "contentdisposition";
	public static final String STATE_KEY = "state";

	private final T payload;
	private final Map<String, Object> headers = new HashMap<>();
	private MessageHeaders messageHeaders;

	protected MessageBase(T payload, MimeType defaultMimeType) {
		this.payload = payload;
		setStatus(200);
		setMimeType(defaultMimeType);
	}

	public void setStatus(int status) {
		if(status < 200 || status > 599) {
			throw new IllegalArgumentException("Status code ["+status+"] must be between 200 and 599");
		}
		setHeader(STATUS_KEY, String.valueOf(status));
	}

	protected void setMimeType(MimeType mimeType) {
		if(mimeType != null) {
			setHeader(MIMETYPE_KEY, mimeType.toString());
		}
	}

	public void setFilename(String filename) {
		setFilename("attachment", filename);
	}

	public void setFilename(String disposition, String filename) {
		setHeader(CONTENT_DISPOSITION_KEY, disposition + "; filename=\""+filename+"\"");
	}

	public void setHeader(String key, String value) {
		headers.put(BusMessageUtils.HEADER_PREFIX+key, value);
	}

	@Override
	public T getPayload() {
		return payload;
	}

	@Override
	public MessageHeaders getHeaders() {
		if(messageHeaders == null) {
			messageHeaders = new MessageHeaders(headers);
		}

		return messageHeaders;
	}
}
