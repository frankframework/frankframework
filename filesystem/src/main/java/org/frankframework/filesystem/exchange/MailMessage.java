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

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import jakarta.mail.internet.InternetAddress;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
@Setter
public class MailMessage extends MailItemId {
	private static final String MESSAGE_BASE = "%s/messages";

	public MailMessage() {
		// public constructor for Jackson
	}

	public MailMessage(MailFolder mailFolder, String id) {
		log.debug("creating new MailItem with id [{}] in folder [{}]", id, mailFolder);
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
	private EmailAddressHolder sender;
	private EmailAddressHolder from;

	public EmailAddress getSender() {
		if (sender == null) {
			return null;
		}
		return sender.getEmailAddress();
	}

	public EmailAddress getFrom() {
		if (from == null) {
			return null;
		}
		return from.getEmailAddress();
	}

	private List<EmailAddressHolder> toRecipients;
	private List<EmailAddressHolder> ccRecipients;
	private List<EmailAddressHolder> bccRecipients;

	public List<EmailAddress> getToRecipients() {
		return unwrapHolder(toRecipients);
	}
	public List<EmailAddress> getCcRecipients() {
		return unwrapHolder(ccRecipients);
	}
	public List<EmailAddress> getBccRecipients() {
		return unwrapHolder(bccRecipients);
	}

	private List<EmailAddress> unwrapHolder(List<EmailAddressHolder> mailAddresses) {
		if (mailAddresses == null) {
			return Collections.emptyList();
		}
		return mailAddresses.stream().map(EmailAddressHolder::getEmailAddress).toList();
	}

	private String replyTo;

	@Override
	public String toString() {
		return "MailItem [%s] with subject [%s] in %s".formatted(getId(), getSubject(), getMailFolder());
	}

	public static class MailBody {
		@Getter @Setter
		private String contentType;

		@Getter @Setter
		private String content;
	}

	public static class EmailAddressHolder {
		@Getter @Setter private EmailAddress emailAddress;
	}

	public static class EmailAddress {
		private @Setter String name;
		private @Setter String address;

		public String get() {
			try {
				InternetAddress iaddress = new InternetAddress(address, name);
				return iaddress.toUnicodeString();
			} catch (UnsupportedEncodingException e) {
				return address.toString();
			}
		}

		@Override
		public String toString() {
			return "EmailAddress: "+get();
		}
	}
}
