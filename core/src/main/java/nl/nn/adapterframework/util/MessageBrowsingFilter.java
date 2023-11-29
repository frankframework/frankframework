/*
   Copyright 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.RawMessageWrapper;

public class MessageBrowsingFilter {
	private String type = null;
	private String host = null;
	private String id = null;
	private String messageId = null;
	private String correlationId = null;
	private String comment = null;
	private String message = null;
	private String label = null;
	private @Getter Date startDate = null;
	private @Getter Date endDate = null;

	private @Getter int maxMessages = 100;
	private @Getter int skipMessages = 0;

	private @Getter SortOrder sortOrder = SortOrder.NONE;
	private IMessageBrowser<?> storage = null;
	private IListener listener = null;

	public MessageBrowsingFilter() {
		this(AppConstants.getInstance().getInt("browse.messages.max", 100), 0);
	}

	public MessageBrowsingFilter(int maxMessages, int skipMessages) {
		this.maxMessages = maxMessages;
		this.skipMessages = skipMessages;
	}


	public boolean matchAny(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
		int count = 0;
		int matches = 0;

		if(type != null) {
			count++;
			matches += iterItem.getType().startsWith(type) ? 1 : 0;
		}
		if(host != null) {
			count++;
			matches += iterItem.getHost().startsWith(host) ? 1 : 0;
		}
		if(id != null) {
			count++;
			matches += iterItem.getId().startsWith(id) ? 1 : 0;
		}
		if(messageId != null) {
			count++;
			matches += iterItem.getOriginalId().startsWith(messageId) ? 1 : 0;
		}
		if(correlationId != null) {
			count++;
			matches += iterItem.getCorrelationId().startsWith(correlationId) ? 1 : 0;
		}
		if(comment != null) {
			count++;
			matches += (StringUtils.isNotEmpty(iterItem.getCommentString()) && iterItem.getCommentString().contains(comment)) ? 1 : 0;
		}
		if(label != null) {
			count++;
			matches += StringUtils.isNotEmpty(iterItem.getLabel()) && iterItem.getLabel().startsWith(label) ? 1 : 0;
		}
		if(startDate != null && endDate == null) {
			count++;
			matches += !iterItem.getInsertDate().before(startDate) ? 1 : 0;
		}
		if(startDate == null && endDate != null) {
			count++;
			matches += !iterItem.getInsertDate().after(endDate) ? 1 : 0;
		}
		if(startDate != null && endDate != null) {
			count++;
			matches += !iterItem.getInsertDate().before(startDate) && !iterItem.getInsertDate().after(endDate) ? 1 : 0;
		}
		if(message != null) {
			count++;
			matches += matchMessage(iterItem) ? 1 : 0;
		}

		return count == matches;
	}

	public void setTypeMask(String typeMask) {
		if(!StringUtils.isEmpty(typeMask))
			type = typeMask;
	}

	public void setHostMask(String hostMask) {
		if(!StringUtils.isEmpty(hostMask))
			host = hostMask;
	}

	public void setIdMask(String idMask) {
		if(!StringUtils.isEmpty(idMask))
			id = idMask;
	}

	public void setMessageIdMask(String messageIdMask) {
		if(!StringUtils.isEmpty(messageIdMask))
			messageId = messageIdMask;
	}

	public void setCorrelationIdMask(String correlationIdMask) {
		if(!StringUtils.isEmpty(correlationIdMask))
			correlationId = correlationIdMask;
	}

	public void setCommentMask(String commentMask) {
		if(!StringUtils.isEmpty(commentMask))
			comment = commentMask;
	}

	public boolean matchMessage(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
		if(message != null) {
			String msg = getMessageText(storage, listener, iterItem.getId());
			if (!StringUtils.containsIgnoreCase(msg, message)) {
				return false;
			}
		}
		return true;
	}

	private String getMessageText(IMessageBrowser<?> messageBrowser, IListener listener, String messageId) throws ListenerException, IOException {
		RawMessageWrapper<?> rawmsg = messageBrowser.browseMessage(messageId);
		return MessageBrowsingUtil.getMessageText(rawmsg, listener);
	}

	public void setMessageMask(String messageMask, IMessageBrowser<?> storage) {
		setMessageMask(messageMask, storage, null);
	}

	public void setMessageMask(String messageMask, IMessageBrowser<?> storage, IListener <?> listener) {
		if(StringUtils.isNotEmpty(messageMask)) {
			this.message = messageMask;
			this.storage = storage;
			this.listener = listener;
		}
	}

	public void setLabelMask(String labelMask) {
		if(!StringUtils.isEmpty(labelMask))
			label = labelMask;
	}

	public void setStartDateMask(String startDateMask) {
		if(!StringUtils.isEmpty(startDateMask)) {
			try {
				startDate = DateFormatUtils.parseAnyDate(startDateMask);
			}
			catch(CalendarParserException ex) {
				throw new IllegalStateException("could not parse date from ["+startDateMask+"]", ex);
			}
		}
	}

	public void setEndDateMask(String endDateMask) {
		if(!StringUtils.isEmpty(endDateMask)) {
			try {
				endDate = DateFormatUtils.parseAnyDate(endDateMask);
			}
			catch(CalendarParserException ex) {
				throw new IllegalStateException("could not parse date from ["+endDateMask+"]", ex);
			}
		}
	}

	public void setSortOrder(SortOrder sortOrder) {
		if(sortOrder != null) {
			this.sortOrder = sortOrder;
		}
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
//		return (new ReflectionToStringBuilder(this) {
//			protected boolean accept(Field f) {
//				return super.accept(f) && !f.getName().equals("passwd");
//			}
//		}).toString();
	}
}
