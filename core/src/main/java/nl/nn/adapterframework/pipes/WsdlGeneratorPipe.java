/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.io.IOException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.soap.WsdlGenerator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Generate WSDL of parent or specified adapter.
 *

 * @author Jaco de Groot
 */
@ElementType(ElementTypes.SESSION)
public class WsdlGeneratorPipe extends FixedForwardPipe {
	private String from = "parent";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (!"parent".equals(getFrom()) && !"input".equals(getFrom())) {
			throw new ConfigurationException("from should either be parent or input");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result = null;
		Adapter adapter;
		try {
			if ("input".equals(getFrom())) {
				String adapterName = message.asString();
				adapter = ((Configuration) getApplicationContext()).getRegisteredAdapter(adapterName);
				if (adapter == null) {
					throw new PipeRunException(this, "Could not find adapter: " + adapterName);
				}
			} else {
				adapter = getPipeLine().getAdapter();
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "Could not determine adapter name", e);
		}
		try {
			String generationInfo = "at " + RestListenerUtils.retrieveRequestURL(session);
			WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), generationInfo);
			wsdl.init();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			wsdl.wsdl(outputStream, null);
			result = outputStream.toString(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (Exception e) {
			throw new PipeRunException(this, "Could not generate WSDL for adapter [" + adapter.getName() + "]", e);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	public String getFrom() {
		return from;
	}

	/**
	 * either parent (adapter of pipeline which contains this pipe) or input (name of adapter specified by input of pipe), adapter must be within the same Configuration
	 * @ff.default parent
	 */
	public void setFrom(String from) {
		this.from = from;
	}

}