/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.mail.Part;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public class PartMessage extends Message {

	private static final long serialVersionUID = 4740404985426114492L;

	private transient Part part;

	public PartMessage(Part part) throws MessagingException {
		this(new MessageContext(), part);
	}

	public PartMessage(Part part, String charset) throws MessagingException {
		this(new MessageContext(charset), part);
	}

	public PartMessage(Part part, Map<String,Object> context) throws MessagingException {
		this(context instanceof MessageContext ? (MessageContext)context : context==null ? new MessageContext() : new MessageContext(context), part);
	}
	private PartMessage(MessageContext context, Part part) throws MessagingException {
		super(part::getInputStream, context.withName(part.getFileName()), part.getClass());
		this.part = part;

		String charset = (String)context.get(MessageContext.METADATA_CHARSET);
		if (StringUtils.isEmpty(charset)) { //if not explicitly set
			try {
				context.withMimeType(part.getContentType());
			} catch (Exception e) {
				log.warn("Could not determine charset", e);
			}
		}
	}

	@Override
	public long size() {
		if (part!=null) {
			try {
				return part.getSize();
			} catch (MessagingException e) {
				log.warn("Cannot get size", e);
				return -1;
			}
		}
		return super.size();
	}

}
