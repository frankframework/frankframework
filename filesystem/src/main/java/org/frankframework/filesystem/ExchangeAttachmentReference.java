/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.filesystem;

import jakarta.annotation.Nonnull;

import lombok.Getter;
import microsoft.exchange.webservices.data.property.complex.Attachment;

/**
 * Reference to an attachment of an Exchange message, together with the {@link ExchangeObjectReference} to the
 * mailbox from which the attachment originated.
 */
public class ExchangeAttachmentReference {
	private final @Getter @Nonnull ExchangeObjectReference mailFolder;
	private final @Getter @Nonnull Attachment attachment;

	private ExchangeAttachmentReference(final @Nonnull ExchangeObjectReference mailFolder, @Nonnull Attachment attachment) {
		this.mailFolder = mailFolder;
		this.attachment = attachment;
	}

	public static ExchangeAttachmentReference of(final @Nonnull ExchangeObjectReference mailFolder, @Nonnull Attachment attachment) {
		return new ExchangeAttachmentReference(mailFolder, attachment);
	}
}
