/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import java.io.IOException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Forward;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;
import org.frankframework.validation.AbstractXmlValidator.ValidationResult;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 * If <code>root</code> is given then this is also checked.
 *
 * @author  Peter Leeuwenburgh
 * @since	4.4.5
 */
@Forward(name = "parserError", description = "a parser exception occurred, probably caused by non-well-formed XML. If not specified, \"failure\" is used in such a case")
@Forward(name = "failure", description = "the document is not well formed")
public class XmlWellFormedCheckerPipe extends FixedForwardPipe implements IValidator {
	private String root = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		registerEvent(ValidationResult.PARSER_ERROR.getEvent());
		registerEvent(ValidationResult.VALID.getEvent());
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		return validate(message, session, getRoot());
	}

	@Override
	public PipeRunResult validate(Message message, PipeLineSession session, String messageRoot) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (XmlUtils.isWellFormed(input, messageRoot)) {
			throwEvent(ValidationResult.VALID.getEvent());
			return new PipeRunResult(getSuccessForward(), message);
		}
		throwEvent(ValidationResult.PARSER_ERROR.getEvent());
		PipeForward forward = findForward("parserError");
		if (forward==null) {
			forward = findForward("failure");
		}
		return new PipeRunResult(forward, message);
	}

	/** name of the root element */
	public void setRoot(String root) {
		this.root = root;
	}
	public String getRoot() {
		return root;
	}

}
