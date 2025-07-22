/*
   Copyright 2021 - 2024 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;
import java.io.Serial;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;

public class PartMessage extends Message {

	@Serial
	private static final long serialVersionUID = 4740404985426114492L;

	private static final Logger LOG = LogManager.getLogger(PartMessage.class);

	public PartMessage(Part part) throws MessagingException, IOException {
		this(part, MultipartUtils.getContext(part));
	}

	public PartMessage(Part part, String charset) throws MessagingException, IOException {
		this(part, MultipartUtils.getContext(part).withCharset(charset));
	}

	public PartMessage(Part part, MessageContext context) throws MessagingException, IOException {
		super(part.getInputStream(), context, part.getClass());

		String charset = (String)context.get(MessageContext.METADATA_CHARSET);
		if (StringUtils.isEmpty(charset)) { // If not explicitly set
			try {
				context.withMimeType(part.getContentType());
			} catch (Exception e) {
				LOG.warn("Could not determine charset", e);
			}
		}
	}

}
