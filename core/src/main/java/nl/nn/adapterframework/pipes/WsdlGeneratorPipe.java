/*
   Copyright 2016 Nationale-Nederlanden

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

import java.io.ByteArrayOutputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.soap.Wsdl;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Generate WSDL of parent or specified adapter.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.WsdlGeneratorPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFrom(String) from}</td><td>either parent (adapter of pipeline which contains this pipe) or input (name of adapter specified by input of pipe)</td><td>parent</td></tr>
 * </table>
 * </p>

 * @author Jaco de Groot
 */
public class WsdlGeneratorPipe extends FixedForwardPipe {
	private String from = "parent";

	public void configure() throws ConfigurationException {
		super.configure();
		if (!"parent".equals(getFrom()) && !"input".equals(getFrom())) {
			throw new ConfigurationException(getLogPrefix(null) + " from should either be parent or input");
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String result = null;
		IAdapter adapter;
		if ("input".equals(getFrom())) {
			adapter = ((Adapter)getAdapter()).getConfiguration().getIbisManager().getRegisteredAdapter((String)input);
			if (adapter == null) {
				throw new PipeRunException(this, "Could not find adapter: " + input);
			}
		} else {
			adapter = getPipeLine().getAdapter();
		}
		try {
			Wsdl wsdl = new Wsdl(((Adapter)adapter).getPipeLine());
			wsdl.setDocumentation("Generated at "
				+ AppConstants.getInstance().getResolvedProperty("otap.stage")
				+ "-"
				+ AppConstants.getInstance().getResolvedProperty("otap.side")
				+ " on " + DateUtils.getIsoTimeStamp() + ".");
			wsdl.init();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			wsdl.wsdl(outputStream, null);
			result = outputStream.toString("UTF-8");
		} catch (Exception e) {
			throw new PipeRunException(this,
					"Could not generate WSDL for adapter '" + adapter.getName()
					+ "'", e); 
		}
		return new PipeRunResult(getForward(), result);
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

}