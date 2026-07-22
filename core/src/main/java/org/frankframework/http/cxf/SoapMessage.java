/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.http.cxf;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPException;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.http.mime.MultipartUtils.MultipartMessages;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

/**
 * Wrapper calls that holds everything related to a SOAPMessage.
 */
public class SoapMessage {
	private final @NonNull SoapContext context;
	private final @NonNull MultipartMessages parts;

	private SoapMessage(@NonNull Message message) throws SOAPException {
		this(new MultipartMessages(message));
	}

	private SoapMessage(@NonNull MultipartMessages parts) throws SOAPException {
		this.parts = parts;
		Message body = parts.body();
		if (Message.isEmpty(body)) {
			throw new SOAPException("no soap body found");
		}

		context = new SoapContext(body);
	}

	public static SoapMessage from(@NonNull HttpServletRequest request) throws SOAPException {
		try {
			if (!MultipartUtils.isMultipart(request)) {
				return new SoapMessage(MessageUtils.parseContentAsMessage(request));
			}

			return new SoapMessage(MultipartUtils.parseMultipart(request.getInputStream(), request.getContentType(), null));
		} catch (IOException e) {
			throw new SOAPException("failed to parse request", e);
		}
	}

	public @Nullable Map<String, Message> getAttachments() {
		return parts.messages();
	}

	public @NonNull SoapContext getSoapContext() {
		return context;
	}

	public @Nullable Object getMultipartXml() {
		return parts.multipartXml();
	}

	public @NonNull Message getBody() {
		return parts.body();
	}
}
