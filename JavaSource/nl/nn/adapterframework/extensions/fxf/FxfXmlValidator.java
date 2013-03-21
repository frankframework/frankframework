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
package nl.nn.adapterframework.extensions.fxf;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.WsdlXmlValidator;

/**
 * FxF XML validator to be used with FxF3. When receiving files
 * (direction=receive) the message is validated against the
 * OnCompletedTransferNotify WSDL (a P2P connection, hence same WSDL (provided
 * by Tibco) for all queues (every Ibis receiving FxF files has it's own
 * queue)). When sending files (direction=send) the message is validated against
 * the StartTransfer WSDL (ESB service provided by Tibco).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.fxf.FxfListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>send</code> or <code>receive</code></td><td>send</td></tr>
 * </table>
 * 
 * @author Jaco de Groot
 */
public class FxfXmlValidator extends WsdlXmlValidator {
	private String direction = "send";

	@Override
	public void configure() throws ConfigurationException {
		setThrowException(true);
		if (getDirection().equals("receive")) {
			setWsdl("xml/wsdl/OnCompletedTransferNotify_FxF3_1.1.4_abstract.wsdl");
			setSoapBody("OnCompletedTransferNotify_Action");
		} else {
			setWsdl("xml/wsdl/StartTransfer_FxF3_1.1.4_abstract.wsdl");
			setSoapHeader("MessageHeader");
			setSoapBody("StartTransfer_Action");
		}
		super.configure();
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

}
