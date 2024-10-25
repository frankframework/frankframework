/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.extensions.fxf;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.pipes.WsdlXmlValidator;

/**
 * FxF XML validator to be used with FxF3. When receiving files
 * (direction=receive) the message is validated against the
 * OnCompletedTransferNotify WSDL (a P2P connection, hence same WSDL (provided
 * by Tibco) for all queues (every Ibis receiving FxF files has it's own
 * queue)). When sending files (direction=send) the message is validated against
 * the StartTransfer WSDL (ESB service provided by Tibco).
 *
 *
 * @author Jaco de Groot
 */
public class FxfXmlValidator extends WsdlXmlValidator {
	private Direction direction = Direction.SEND;
	private @Getter String fxfVersion = "3.1";

	public enum Direction {
		SEND,RECEIVE
	}

	@Override
	public void configure() throws ConfigurationException {
		setThrowException(true);
		if(direction == Direction.RECEIVE) {
			setWsdl("xml/wsdl/OnCompletedTransferNotify_FxF3_1.1.4_abstract.wsdl");
			setSoapBody("OnCompletedTransferNotify_Action");
		} else {
			if ("3.2".equals(fxfVersion)) {
				setWsdl("xml/wsdl/StartTransfer_FxF3v2_abstract.wsdl");
			} else {
				setWsdl("xml/wsdl/StartTransfer_FxF3_1.1.4_abstract.wsdl");
			}
			setSoapHeader("MessageHeader");
			setSoapBody("StartTransfer_Action");
		}
		super.configure();
		if (!getFxfVersion().equals("3.1") && !getFxfVersion().equals("3.2")) {
			throw new ConfigurationException("illegal value for fxfVersion [" + getFxfVersion() + "], must be '3.1' or '3.2'");
		}
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	/**
	 * either 3.1 or 3.2
	 * @ff.default 3.1
	 */
	public void setFxfVersion(String fxfVersion) {
		this.fxfVersion = fxfVersion;
	}
}
