/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.IWrapperPipe;
import nl.nn.adapterframework.doc.Category;

/**
 * Plain extension to {@link MessageSendingPipe} that can be used directly in configurations.
 * Only extension is that the setters for listener and sender have been made public, and can therefore
 * be set from the configuration file.
 *
 * @ff.parameters Any parameters defined on the pipe will be handed to the sender, if this is a ISenderWithParameters.
 *
 * @author  Dennis van Loon
 */
@Category("Basic")
public class SenderPipe extends MessageSendingPipe {

	@Override // to obtain proper order in FrankDoc and XSD
	public void setMessageLog(ITransactionalStorage messageLog) {
		super.setMessageLog(messageLog);
	}

	@Override // to obtain proper order in FrankDoc and XSD
	public void setInputWrapper(IWrapperPipe inputWrapper) {
		super.setInputWrapper(inputWrapper);
	}

	@Override
	public void setInputValidator(IValidator inputValidator) {
		super.setInputValidator(inputValidator);
	}

	@Override
	public void setSender(ISender sender) {
		super.setSender(sender);
	}

	@Override
	public void setListener(ICorrelatedPullingListener listener) {
		super.setListener(listener);
	}

}
