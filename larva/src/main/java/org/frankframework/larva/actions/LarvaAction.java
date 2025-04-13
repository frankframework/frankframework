/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.larva.actions;

import java.util.Map;
import java.util.Properties;

import org.frankframework.core.IConfigurable;
import org.frankframework.core.ListenerException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.larva.FileListener;
import org.frankframework.larva.FileSender;
import org.frankframework.larva.XsltProviderListener;
import org.frankframework.stream.Message;

public class LarvaAction extends AbstractLarvaAction<IConfigurable> {

	public LarvaAction(IConfigurable configurable) {
		super(configurable);
	}

	@Override
	public void executeWrite(Message fileContent, String correlationId, Map<String, Object> parameters) throws TimeoutException, SenderException, ListenerException {
		if (peek() instanceof FileSender fileSender) {
			fileSender.sendMessage(fileContent);
		} else if (peek() instanceof XsltProviderListener xsltProviderListener) {
			xsltProviderListener.processRequest(fileContent, parameters);
		} else {
			throw new SenderException("could not perform write step for queue [" + peek() + "]");
		}
	}

	@Override
	public Message executeRead(Properties properties) throws SenderException, TimeoutException, ListenerException {
		if (peek() instanceof FileSender fileSender) {
			return fileSender.getMessage();
		}
		if (peek() instanceof FileListener fileListener) {
			return fileListener.getMessage();
		}
		if (peek() instanceof XsltProviderListener xsltProviderListener) {
			return xsltProviderListener.getResult();
		}
		throw new SenderException("could not perform read step for queue [" + peek() + "]");
	}
}
