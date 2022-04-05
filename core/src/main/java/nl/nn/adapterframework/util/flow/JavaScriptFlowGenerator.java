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
import java.lang.ref.SoftReference;

import javax.xml.transform.TransformerException;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.flow.graphviz.GraphvizEngine;

/**
 * Initialized through Spring. Uses @{link GraphvizEngine} to get an available 
 * JavaScriptEngine to generate the Flow images with.
 */
public class JavaScriptFlowGenerator implements IFlowGenerator {
	private static Logger log = LogUtil.getLogger(JavaScriptFlowGenerator.class);

	private static final String ADAPTER2DOT_XSLT = "/xml/xsl/adapter2dot.xsl";
	private static final String CONFIGURATION2DOT_XSLT = "/xml/xsl/configuration2dot.xsl";

	private TransformerPool transformerPoolAdapter;
	private TransformerPool transformerPoolConfig;

	/**
	 * Optional IFlowGenerator. If non present the FlowDiagramManager should still be 
	 * able to generate dot files and return the `noImageAvailable` image.
	 */
	private ThreadLocal<SoftReference<GraphvizEngine>> graphvisEngines = new ThreadLocal<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		if(log.isTraceEnabled()) log.trace("creating JavaScriptFlowGenerator");

		Resource xsltSourceConfig = Resource.getResource(ADAPTER2DOT_XSLT);
		transformerPoolAdapter = TransformerPool.getInstance(xsltSourceConfig, 2);

		Resource xsltSourceIbis = Resource.getResource(CONFIGURATION2DOT_XSLT);
		transformerPoolConfig = TransformerPool.getInstance(xsltSourceIbis, 2);
	}

	/**
	 * The IFlowGenerator is wrapped in a SoftReference, wrapped in a ThreadLocal. 
	 * When the thread is cleaned up, it will remove the instance. Or when the GC is 
	 * running out of heapspace, it will remove the IFlowGenerator. This method makes sure,
	 * as long as the IFlowGenerator bean can initialize, always a valid instance is returned.
	 */
	public GraphvizEngine getGraphvizEngine() {
		SoftReference<GraphvizEngine> reference = graphvisEngines.get();
		if(reference == null || reference.get() == null) {
			GraphvizEngine generator = createGraphvizEngine();
			if(generator == null) {
				return null;
			}

			reference = new SoftReference<>(generator);
			graphvisEngines.set(reference);
		}

		return reference.get();
	}

	private GraphvizEngine createGraphvizEngine() {
		try {
			return new GraphvizEngine();
		} catch (Throwable t) {
			log.warn("failed to initalize IFlowGenerator", t);
			return null;
		}
	}

	@Override
	public void generateFlow(String xml, OutputStream outputStream) throws FlowGenerationException {
		String dot = null;
		try {
			if(xml.startsWith("<adapter")) {
				dot = transformerPoolAdapter.transform(xml, null);
			} else {
				dot = transformerPoolConfig.transform(xml, null);
			}
		} catch (IOException | TransformerException | SAXException e) {
			throw new FlowGenerationException("error transforming [xml] to [dot]", e);
		}

		try {
			String flow = getGraphvizEngine().execute(dot);

			outputStream.write(flow.getBytes());
		} catch (IOException e) {
			throw new FlowGenerationException(e);
		}
	}

	@Override
	public String getFileExtension() {
		return "svg";
	}

	@Override
	public MediaType getMediaType() {
		return new MediaType("image", "svg+xml");
	}

	@Override
	public void destroy() {
		if(transformerPoolAdapter != null)
			transformerPoolAdapter.close();

		if(transformerPoolConfig != null)
			transformerPoolConfig.close();

		graphvisEngines.remove();
		if(log.isTraceEnabled()) log.trace("destroyed JavaScriptFlowGenerator");
	}
}
