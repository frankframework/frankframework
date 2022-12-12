/*
   Copyright 2022 WeAreFrank!

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

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.logging.log4j.ThreadContext;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentBuilderFactory;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.stream.document.ObjectBuilder;

public class RetrieveContextPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try (ObjectBuilder document = DocumentBuilderFactory.startObjectDocument(DocumentFormat.JSON, "context")) {
			document.add("message", message.asString());
			try (ObjectBuilder messageContext = document.addObjectField("messageContext")) {
				for (Entry<String,Object> entry:message.getContext().entrySet()) {
					messageContext.add(entry.getKey(), entry.getValue().toString());
				}
			}
			try (ObjectBuilder logContext = document.addObjectField("logContext")) {
				for (Entry<String,String> entry:ThreadContext.getContext().entrySet()) {
					logContext.add(entry.getKey(), entry.getValue());
				}
			}
			if (getParameterList()!=null && !getParameterList().isEmpty()) {
				try (ObjectBuilder parameters = document.addObjectField("parameters")) {
					for (Entry<String,Object> entry:getParameterList().getValues(message, session).getValueMap().entrySet()) {
						parameters.add(entry.getKey(), entry.getValue().toString());
					}
				} catch (ParameterException e) {
					throw new PipeRunException(this, "Exception caught", e);
				}
			}
			document.close(); // must close documentbuilder before output can be obtained via toString()
			return new PipeRunResult(getSuccessForward(), document.toString());
		} catch (SAXException | IOException e) {
			throw new PipeRunException(this, "Exception caught", e);
		}
	}


}
