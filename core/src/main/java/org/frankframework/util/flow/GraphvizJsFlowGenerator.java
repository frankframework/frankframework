/*
   Copyright 2020-2022 WeAreFrank!

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
package org.frankframework.util.flow;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;

import org.springframework.http.MediaType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.flow.graphviz.GraphvizEngine;

/**
 * Initialized through Spring. Uses {@link GraphvizEngine} to get an available
 * JavaScriptEngine to generate the Flow images with.
 */
@Log4j2
public class GraphvizJsFlowGenerator extends DotFlowGenerator {

	private static final ThreadLocal<SoftReference<GraphvizEngine>> GRAPHVIZ_ENGINES = new ThreadLocal<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		try {
			GraphvizEngine engine = new GraphvizEngine(); // Verify we can create a 'JavascriptEngine'
			engine.close();
		} catch (Throwable t) {
			log.warn("failed to initalize GraphvizEngine", t);
			throw t;
		}
	}

	/**
	 * The IFlowGenerator is wrapped in a SoftReference, wrapped in a ThreadLocal.
	 * When the thread is cleaned up, it will remove the instance. Or when the GC is
	 * running out of heapspace, it will remove the IFlowGenerator. This method makes sure,
	 * as long as the IFlowGenerator bean can initialize, always a valid instance is returned.
	 */
	protected GraphvizEngine getGraphvizEngine() {
		SoftReference<GraphvizEngine> reference = GRAPHVIZ_ENGINES.get();
		if(reference == null || reference.get() == null) {
			GraphvizEngine generator = createGraphvizEngine();
			if(generator == null) {
				return null;
			}

			reference = new SoftReference<>(generator);
			GRAPHVIZ_ENGINES.set(reference);
		}

		return reference.get();
	}

	private GraphvizEngine createGraphvizEngine() {
		try {
			return new GraphvizEngine();
		} catch (Throwable t) {
			return null;
		}
	}

	@Override
	public void generateFlow(String xml, OutputStream outputStream) throws FlowGenerationException {
		GraphvizEngine engine = getGraphvizEngine();
		if(engine != null) {
			String dot = generateDot(xml);

			try {
				String flow = engine.execute(dot);

				outputStream.write(flow.getBytes());
			} catch (IOException e) {
				throw new FlowGenerationException(e);
			}
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
		super.destroy();

		GRAPHVIZ_ENGINES.remove();
	}
}
