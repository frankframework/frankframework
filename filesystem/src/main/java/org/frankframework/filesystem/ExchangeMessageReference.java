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
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;

/**
 * This is a wrapper around an Exchange {@link EmailMessage}, and a reference to the mailbox from which the message
 * was read.
 * <p/>
 * This wrapper class is so that we can restore the connection to the correct mailbox in later processing.
 */
public class ExchangeMessageReference {
	private final @Getter @Nonnull ExchangeObjectReference mailFolder;
	private final @Getter @Nonnull EmailMessage message;

	private ExchangeMessageReference(final @Nonnull ExchangeObjectReference mailFolder, final @Nonnull EmailMessage message) {
		this.mailFolder = mailFolder;
		this.message = message;
	}

	public String getMailAddress() {
		return mailFolder.getMailbox();
	}

	public static ExchangeMessageReference of(final @Nonnull ExchangeObjectReference mailFolder, final @Nonnull EmailMessage message) {
		return new ExchangeMessageReference(mailFolder, message);
	}
}
