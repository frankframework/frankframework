/*
   Copyright 2013, 2020 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.validation.AbstractXmlValidator;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 * If <code>root</code> is given then this is also checked.
 * 
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td>"parserError"</td><td>a parser exception occurred, probably caused by non-well-formed XML. If not specified, "failure" is used in such a case</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred</td></tr>
 * </table>
 * <br>
 * @author  Peter Leeuwenburgh
 * @since	4.4.5
 */
public class XmlWellFormedChecker extends FixedForwardPipe implements IValidator {
	private String root = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
		registerEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
	}


	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}
		if (XmlUtils.isWellFormed(input, getRoot())) {
			throwEvent(AbstractXmlValidator.XML_VALIDATOR_VALID_MONITOR_EVENT);
			return new PipeRunResult(getForward(), message);
		}
		throwEvent(AbstractXmlValidator.XML_VALIDATOR_PARSER_ERROR_MONITOR_EVENT);
		PipeForward forward = findForward("parserError");
		if (forward==null) {
			forward = findForward("failure");
		}
		return new PipeRunResult(forward, message);
	}

	@IbisDoc({"name of the root element", ""})
	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
