/*
   Copyright 2020-2021 WeAreFrank!

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

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.flow.graphviz.Format;
import nl.nn.adapterframework.util.flow.graphviz.GraphvizEngine;
import nl.nn.adapterframework.util.flow.graphviz.Options;

/**
 * Initialized through Spring. Uses @{link GraphvizEngine} to get an available 
 * JavaScriptEngine to generate the Flow images with.
 */
public class JavaScriptFlowGenerator implements IFlowGenerator {
	private static Logger log = LogUtil.getLogger(JavaScriptFlowGenerator.class);

	private GraphvizEngine engine;
	private Options options = Options.create();
	private String jsFormat = AppConstants.getInstance().getProperty("graphviz.js.format", "SVG");

	@Override
	public void afterPropertiesSet() throws Exception {
		if(log.isTraceEnabled()) log.trace("creating JavaScriptFlowGenerator");
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
		} catch (FlowGenerationException e) {
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
		if(log.isTraceEnabled()) log.trace("destroyed JavaScriptFlowGenerator");
	}

	/**
	 * The {@link FlowDiagramManager} uses a ThreadLocal+SoftReference map to cache the 
	 * {@link IFlowGenerator FlowGenerators}. This method ensures that the engine is destroyed properly.
	 */
	@Override
	protected void finalize() throws Throwable {
		destroy();
		super.finalize();
	}
}
