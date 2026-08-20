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
package org.frankframework.filesystem.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.frankframework.util.JacksonUtils;

public class MailMessageMapperTest {

	/**
	 * Users reported an issue that mapping failed with a `replyTo` set in a received e-mail. This test ensures that a an actual captured JSON response
	 * can be parsed into a MailMessage.
	 */
	@Test
	void testAllLongFieldsCanExceedIntegerMaxValue() {
		String json = """
				{
				  "id": "dummy-id",
				  "createdDateTime": "2026-08-19T09:26:42Z",
				  "lastModifiedDateTime": "2026-08-19T09:26:42Z",
				  "changeKey": "dummy",
				  "categories": [],
				  "receivedDateTime": "2026-08-19T09:26:42Z",
				  "sentDateTime": "2026-08-19T09:26:42Z",
				  "hasAttachments": false,
				  "internetMessageId": "fake-id",
				  "subject": "rawMessageFile",
				  "bodyPreview": "Test Message Contents",
				  "importance": "normal",
				  "parentFolderId": "fake-parent-id",
				  "conversationId": "fake-conversation-id",
				  "conversationIndex": "fake-idx",
				  "isDeliveryReceiptRequested": false,
				  "isReadReceiptRequested": false,
				  "isRead": true,
				  "isDraft": true,
				  "webLink": "https://example.com/exampleMessageLink",
				  "inferenceClassification": "focused",
				  "body": {
				    "contentType": "text",
				    "content": "Test Message Contents"
				  },
				  "toRecipients": [
				    {
				      "emailAddress": {
				        "name": "Sergi Philipsen",
				        "address": "sergi@frankframework.org"
				      }
				    }
				  ],
				  "ccRecipients": [],
				  "bccRecipients": [],
				  "replyTo": [
				    {
				      "emailAddress": {
				        "name": "iaf-test-mailbox",
				        "address": "frank-test-mailbox@frankframework.org"
				      }
				    }
				  ],
				  "flag": {
				    "flagStatus": "notFlagged"
				  }
				}
				""";

		MailMessage mailMessage = JacksonUtils.convertToDTO(json, MailMessage.class);

		MailMessage.EmailAddress recipient = mailMessage.getToRecipients().getFirst();
		assertThat(recipient.get()).isEqualTo("Sergi Philipsen <sergi@frankframework.org>");

		MailMessage.EmailAddress replyTo = mailMessage.getReplyTo().getFirst();
		assertThat(replyTo.get()).isEqualTo("iaf-test-mailbox <frank-test-mailbox@frankframework.org>");
	}
}
