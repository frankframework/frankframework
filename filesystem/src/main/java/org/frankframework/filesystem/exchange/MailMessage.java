/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.filesystem.exchange;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailMessage extends MailItemId {
	private static final String MESSAGE_BASE = "%s/messages";

	public MailMessage() {
		// public constructor for Jackson
	}

	public MailMessage(MailFolder mailFolder, String id) {
		setMailFolder(mailFolder);
		setId(id);
	}

	/**
	 * Also sets the baseUrl for this message to the url of the `folder` that's provided.
	 */
	@Override
	public void setMailFolder(MailFolder mailFolder) {
		super.setMailFolder(mailFolder);
		setUrl(MESSAGE_BASE.formatted(mailFolder.getUrl()));
	}

	private String sentDateTime;
	private String createdDateTime;
	private String receivedDateTime;
	private String lastModifiedDateTime;
	private boolean hasAttachments;

	private String subject;
	private String importance;
	private String conversationId;
	private boolean isDeliveryReceiptRequested;
	private boolean isReadReceiptRequested;
	private boolean isRead;
	private boolean isDraft;
	private MailBody body;
	private EmailAddress sender;
	private EmailAddress from;
	private List<EmailAddress> toRecipients;
	private List<EmailAddress> ccRecipients;
	private List<EmailAddress> bccRecipients;
	private String replyTo;

	@Getter
	@Setter
	public static class MailBody {
		private String contentType;
		private String content;
	}

	@Getter
	@Setter
	public static class EmailAddress {
		private String name;
		private String address;
	}
}
