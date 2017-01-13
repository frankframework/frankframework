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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.PipeAware;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import javax.xml.xpath.XPathExpressionException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;

/**
 * Performs a reload on database config .
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author  Lars Sinke
 */
public class ReloadSender extends SenderWithParametersBase implements PipeAware {

	private AbstractPipe pipe;

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {

		String configName = null;
		String activeVersion = null;

		try {
			configName = XmlUtils.evaluateXPathNodeSetFirstElement(message,
					"row/field[@name='name']");
		} catch (DomBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			activeVersion = XmlUtils.evaluateXPathNodeSetFirstElement(message,
					"row/field[@name='version']");
		} catch (DomBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Configuration configuration = getPipe().getAdapter().getConfiguration()
				.getIbisManager().getConfiguration(configName);

		String latestVersion = configuration.getVersion();

		if (!latestVersion.equals(activeVersion)) {
			IbisContext ibisContext = getPipe().getAdapter().getConfiguration()
					.getIbisManager().getIbisContext();
			ibisContext.reload(configName);
			return "Reload " + configName + " succeeded";
		} else {
			return "Reload " + configName + " skipped";
		}
	}

	public void setPipe(AbstractPipe pipe) {
		this.pipe = pipe;
	}

	public AbstractPipe getPipe() {
		return pipe;
	}
};