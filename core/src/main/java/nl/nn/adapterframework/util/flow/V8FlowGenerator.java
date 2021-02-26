/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.util.flow;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.extensions.graphviz.Format;
import nl.nn.adapterframework.extensions.graphviz.GraphvizEngine;
import nl.nn.adapterframework.extensions.graphviz.GraphvizException;
import nl.nn.adapterframework.extensions.graphviz.Options;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class V8FlowGenerator implements IFlowGenerator {
	private static Logger log = LogUtil.getLogger(V8FlowGenerator.class);

	private GraphvizEngine engine;
	private Options options = Options.create();
	private String jsFormat = AppConstants.getInstance().getProperty("graphviz.js.format", "SVG");

	@Override
	public void afterPropertiesSet() throws Exception {
		if(log.isTraceEnabled()) log.trace("creating V8FlowEngine");
		engine = new GraphvizEngine();

		Format format;
		try {
			format = Format.valueOf(jsFormat.toUpperCase());
		}
		catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("unknown format["+jsFormat.toUpperCase()+"], must be one of "+Format.values());
		}

		options = options.format(format);

		if(log.isDebugEnabled()) log.debug("Setting Graphviz options to ["+options+"]");
	}

	@Override
	public void generateFlow(String name, String dot, OutputStream outputStream) throws IOException {
		try {
			String flow = engine.execute(dot, options);

			outputStream.write(flow.getBytes());
		} catch (GraphvizException e) {
			throw new IOException (e);
		}
	}

	@Override
	public void setFileExtension(String jsFormat) {
		if(StringUtils.isNotEmpty(jsFormat)) {
			this.jsFormat = jsFormat;
		}
	}

	@Override
	public String getFileExtension() {
		return jsFormat.toLowerCase();
	}

	@Override
	public void destroy() {
		engine.close();
		if(log.isTraceEnabled()) log.trace("destroyed V8FlowEngine");
	}

	@Override
	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}
}
