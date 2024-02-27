/*
   Copyright 2021, 2022, 2024 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;

public class PartMessage extends Message {

	private static final long serialVersionUID = 4740404985426114492L;

	private static final Logger LOG = LogManager.getLogger(Message.class);

	private final transient Part part;

	public PartMessage(Part part) throws MessagingException {
		this(part, new MessageContext());
	}

	public PartMessage(Part part, String charset) throws MessagingException {
		this(part, new MessageContext(charset));
	}

	public PartMessage(Part part, MessageContext context) throws MessagingException {
		super(part::getInputStream, context.withName(part.getFileName()), part.getClass());
		this.part = part;

		String charset = (String)context.get(MessageContext.METADATA_CHARSET);
		if (StringUtils.isEmpty(charset)) { //if not explicitly set
			try {
				context.withMimeType(part.getContentType());
			} catch (Exception e) {
				LOG.warn("Could not determine charset", e);
			}
		}
	}

	@Override
	public long size() {
		if (part!=null) {
			try {
				return part.getSize();
			} catch (MessagingException e) {
				LOG.warn("Cannot get size", e);
				return -1;
			}
		}

		return super.size();
	}

}
