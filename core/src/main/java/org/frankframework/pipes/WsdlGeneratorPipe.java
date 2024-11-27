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
package org.frankframework.pipes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.http.RestListenerUtils;
import org.frankframework.soap.WsdlGenerator;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

/**
 * Generate WSDL of parent or specified adapter.
 *

 * @author Jaco de Groot
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
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
