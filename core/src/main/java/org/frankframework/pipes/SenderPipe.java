/*
   Copyright 2013 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.pipes;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.ISender;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWrapperPipe;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Reintroduce;
import org.frankframework.jdbc.MessageStoreSender;
import org.frankframework.senders.IbisLocalSender;
import org.frankframework.util.AppConstants;

/**
 * Sends a message using an {@link ISender sender} and optionally receives a reply from the same sender.
 *
 * {@inheritClassDoc}
 */
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ENDPOINT)
public class SenderPipe extends MessageSendingPipe {

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getMessageLog() == null && StringUtils.isEmpty(getStubFilename()) && !getSender().isSynchronous()
				&& !(getSender() instanceof IbisLocalSender)
				&& !(getSender() instanceof MessageStoreSender)) { // sender is asynchronous and not a local sender or messageStoreSender, but has no messageLog
			boolean suppressIntegrityCheckWarning = ConfigurationWarnings.isSuppressed(SuppressKeys.INTEGRITY_CHECK_SUPPRESS_KEY, getAdapter());
			if (!suppressIntegrityCheckWarning) {
				boolean legacyCheckMessageLog = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("messageLog.check", true);
				if (!legacyCheckMessageLog) {
					ConfigurationWarnings.add(this, log, "Suppressing integrityCheck warnings by setting property 'messageLog.check=false' has been replaced by by setting property 'warnings.suppress.integrityCheck=true'");
					suppressIntegrityCheckWarning=true;
				}
			}
			if (!suppressIntegrityCheckWarning) {
				ConfigurationWarnings.add(this, log, "asynchronous sender [" + getSender().getName() + "] has no messageLog. " +
					"Service Managers will not be able to perform an integrity check (matching messages received by the adapter to messages sent by this pipe). " +
					"This warning can be suppressed globally by setting property 'warnings.suppress.integrityCheck=true', "+
					"or for this adapter only by setting property 'warnings.suppress.integrityCheck."+getAdapter().getName()+"=true'");
			}
		}
	}

	@Override
	@Reintroduce
	public void setMessageLog(ITransactionalStorage<?> messageLog) {
		super.setMessageLog(messageLog);
	}

	@Override
	@Reintroduce
	public void setInputWrapper(IWrapperPipe inputWrapper) {
		super.setInputWrapper(inputWrapper);
	}

	@Override
	@Reintroduce
	public void setInputValidator(IValidator inputValidator) {
		super.setInputValidator(inputValidator);
	}

	@Override
	@Reintroduce
	public void setSender(ISender sender) {
		super.setSender(sender);
	}
}
