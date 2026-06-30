/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import lombok.Getter;

import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowser.SortOrder;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.receivers.RawMessageWrapper;

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
	private IListener<?> listener = null;

	public MessageBrowsingFilter() {
		this(AppConstants.getInstance().getInt("browse.messages.max", 100), 0);
	}

	public MessageBrowsingFilter(int maxMessages, int skipMessages) {
		this.maxMessages = maxMessages;
		this.skipMessages = skipMessages;
	}


	public boolean matchAll(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
		int count = 0;
		int matches = 0;

		if (StringUtils.isNotEmpty(type)) {
			count++;
			matches += iterItem.getType() != null && iterItem.getType().startsWith(type) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(host)) {
			count++;
			matches += iterItem.getHost() != null && iterItem.getHost().startsWith(host) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(id)) {
			count++;
			matches += iterItem.getId() != null && iterItem.getId().startsWith(id) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(messageId)) {
			count++;
			matches += iterItem.getOriginalId() != null && iterItem.getOriginalId().startsWith(messageId) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(correlationId)) {
			count++;
			matches += iterItem.getCorrelationId() != null && iterItem.getCorrelationId().startsWith(correlationId) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(comment)) {
			count++;
			matches += StringUtils.isNotEmpty(iterItem.getCommentString()) && iterItem.getCommentString().contains(comment) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(label)) {
			count++;
			matches += StringUtils.isNotEmpty(iterItem.getLabel()) && iterItem.getLabel().startsWith(label) ? 1 : 0;
		}
		if (startDate != null && endDate == null) {
			count++;
			matches += iterItem.getInsertDate() != null && !iterItem.getInsertDate().before(startDate) ? 1 : 0;
		}
		if (startDate == null && endDate != null) {
			count++;
			matches += iterItem.getInsertDate() != null && !iterItem.getInsertDate().after(endDate) ? 1 : 0;
		}
		if (startDate != null && endDate != null) {
			count++;
			matches += iterItem.getInsertDate() != null && !iterItem.getInsertDate().before(startDate) && !iterItem.getInsertDate().after(endDate) ? 1 : 0;
		}
		if (StringUtils.isNotEmpty(message)) {
			count++;
			matches += matchMessage(iterItem) ? 1 : 0;
		}

		return count == matches;
	}

	public void setTypeMask(String typeMask) {
		if(StringUtils.isNotEmpty(typeMask))
			type = typeMask;
	}

	public void setHostMask(String hostMask) {
		if(StringUtils.isNotEmpty(hostMask))
			host = hostMask;
	}

	public void setIdMask(String idMask) {
		if(StringUtils.isNotEmpty(idMask))
			id = idMask;
	}

	public void setMessageIdMask(String messageIdMask) {
		if(StringUtils.isNotEmpty(messageIdMask))
			messageId = messageIdMask;
	}

	public void setCorrelationIdMask(String correlationIdMask) {
		if(StringUtils.isNotEmpty(correlationIdMask))
			correlationId = correlationIdMask;
	}

	public void setCommentMask(String commentMask) {
		if(StringUtils.isNotEmpty(commentMask))
			comment = commentMask;
	}

	public boolean matchMessage(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
		if(message != null) {
			String msg = getMessageText(storage, listener, iterItem.getId());
			return Strings.CI.contains(msg, message);
		}
		return true;
	}

	private String getMessageText(IMessageBrowser<?> messageBrowser, IListener<?> listener, String messageId) throws ListenerException, IOException {
		RawMessageWrapper<?> rawmsg = messageBrowser.browseMessage(messageId);
		return MessageBrowsingUtil.getMessageText(rawmsg, listener);
	}

	public void setMessageMask(String messageMask, IMessageBrowser<?> storage) {
		setMessageMask(messageMask, storage, null);
	}

	public void setMessageMask(String messageMask, IMessageBrowser<?> storage, IListener <?> listener) {
		if (StringUtils.isNotEmpty(messageMask)) {
			this.message = messageMask;
			this.storage = storage;
			this.listener = listener;
		}
	}

	public void setLabelMask(String labelMask) {
		if(StringUtils.isNotEmpty(labelMask))
			label = labelMask;
	}

	public void setStartDateMask(String startDateMask) {
		if(StringUtils.isNotEmpty(startDateMask)) {
			try {
				startDate = DateFormatUtils.parseAnyDate(startDateMask);
			}
			catch(Exception ex) {
				throw new IllegalStateException("could not parse date from ["+startDateMask+"]", ex);
			}
		}
	}

	public void setEndDateMask(String endDateMask) {
		if(StringUtils.isNotEmpty(endDateMask)) {
			try {
				endDate = DateFormatUtils.parseAnyDate(endDateMask);
			}
			catch(Exception ex) {
				throw new IllegalStateException("could not parse date from ["+endDateMask+"]", ex);
			}
		}
	}

	public void setSortOrder(SortOrder sortOrder) {
		if (sortOrder != null) {
			this.sortOrder = sortOrder;
		}
	}

	@Override
	public String toString() {
		return StringUtil.reflectionToString(this);
	}
}
