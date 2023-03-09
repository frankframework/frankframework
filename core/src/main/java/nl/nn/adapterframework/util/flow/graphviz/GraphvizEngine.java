/*
   Copyright 2018-2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.util.flow.graphviz;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.extensions.javascript.JavascriptEngine;
import nl.nn.adapterframework.extensions.javascript.JavascriptException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.flow.FlowGenerationException;
import nl.nn.adapterframework.util.flow.GraphvizJsFlowGenerator;
import nl.nn.adapterframework.util.flow.ResultHandler;

//TODO: consider moving this to a separate module
/**
 * JavaScript engine wrapper for VizJs flow diagrams
 *
 * @author Niels Meijer
 *
 */
public class GraphvizEngine {
	protected Logger log = LogUtil.getLogger(this);
	private Engine engine;
	private String graphvizVersion = AppConstants.getInstance().getProperty("graphviz.js.version", "2.0.0");
	private String fileFormat = AppConstants.getInstance().getProperty("graphviz.js.format", "SVG");

	// Available JS Engines. Lower index has priority.
	private static String[] engines = AppConstants.getInstance().getString("flow.javascript.engines", "nl.nn.adapterframework.extensions.javascript.J2V8,nl.nn.adapterframework.extensions.javascript.Nashorn").split(",");

	private Options defaultOptions = null;

	/**
	 * Create a new GraphvizEngine instance. Using version 2.0.0
	 * @throws IOException
	 */
	public GraphvizEngine() throws IOException {
		this(null);
	}

	/**
	 * Create a new GraphvizEngine instance
	 * @param graphvizVersion version of the the VisJs engine to initiate
	 * @throws IOException
	 */
	public GraphvizEngine(String graphvizVersion) throws IOException {
		if(StringUtils.isNotEmpty(graphvizVersion)) {
			this.graphvizVersion = graphvizVersion;
		}

		try {
			Format format = Format.valueOf(fileFormat.toUpperCase());
			defaultOptions = Options.create().format(format);
			if(log.isDebugEnabled()) log.debug("Setting Graphviz options to ["+defaultOptions+"]");
		}
		catch(IllegalArgumentException e) {
			throw new IllegalArgumentException("unknown format["+fileFormat.toUpperCase()+"], must be one of "+Format.values());
		}

		//Create the GraphvizEngine, make sure it can find and load the required libraries
		getEngine();
	}

	/**
	 * Execute GraphViz with default options ({@link Format#SVG})
	 * @param src dot file
	 * @return {@link Format#SVG} string
	 * @throws IOException when VizJs files can't be found on the classpath
	 * @throws FlowGenerationException when a JavaScript engine error occurs
	 */
	public String execute(String src) throws IOException, FlowGenerationException {
		return execute(src, defaultOptions);
	}

	/**
	 * Execute GraphViz
	 * @param src dot file
	 * @param options see {@link Options}
	 * @return string in specified {@link Format}
	 * @throws IOException when VizJs files can't be found on the classpath
	 * @throws FlowGenerationException when a JavaScript engine error occurs
	 */
	public String execute(String src, Options options) throws IOException, FlowGenerationException {
		if(StringUtils.isEmpty(src)) {
			throw new FlowGenerationException("no dot-file provided");
		}

		long start = 0;
		if(log.isDebugEnabled()) {
			if(log.isTraceEnabled()) log.trace("executing VizJS src["+src+"] options["+options.toString()+"]");
			start = System.currentTimeMillis();
		}

		String call = jsVizExec(src, options);
		String result = getEngine().execute(call);
		if(start > 0) {
			log.debug("executed VisJs in ["+(System.currentTimeMillis() - start)+"]ms");
		}
		return options.postProcess(result);
	}

	private String jsVizExec(String src, Options options) {
		return src.startsWith("render") ? src : ("render('" + jsEscape(src) + "'," + options.toJson(false) + ");");
	}

	private String jsEscape(String js) {
		return js.replaceAll("\n", " ").replaceAll("\r", "").replace("\\", "\\\\").replace("'", "\\'");
	}

	private String getVizJsSource(String version) throws IOException {
		URL vizWrapperURL = ClassUtils.getResourceURL("/js/viz-" + version + ".js");
		URL vizRenderURL = ClassUtils.getResourceURL("/js/viz-full.render-" + version + ".js");
		if(vizWrapperURL == null || vizRenderURL == null)
			throw new IOException("failed to open vizjs file for version ["+version+"]");
		return StreamUtil.streamToString(vizWrapperURL.openStream()) + StreamUtil.streamToString(vizRenderURL.openStream());
	}


	/**
	 * Creates the GraphvizEngine instance
	 * @throws IOException when the VizJS file can't be found
	 */
	private synchronized Engine getEngine() throws IOException {
		if(null == engine) {
			log.debug("creating new VizJs engine");
			String visJsSource = getVizJsSource(graphvizVersion);
			engine = new Engine(getVisJsWrapper(), visJsSource);
		}
		return engine;
	}

	/**
	 * Shuts down the GraphvizEngine instance
	 */
	public void close() {
		if (engine != null) {
			engine.close();
		}
	}

	/**
	 * The {@link GraphvizJsFlowGenerator} uses a ThreadLocal+SoftReference map to cache the
	 * {@link GraphvizEngine GraphvisEngines}. This method ensures that the engine is destroyed properly.
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	private String getVisJsWrapper() {
		return "var viz = new Viz();"
				+ "function render(src, options){"
				+ "  try {"
				+ "    viz.renderString(src, options)"
				+ "      .then(function(res) { result(res); })"
				+ "      .catch(function(err) { viz = new Viz(); error(err.toString()); });"
				+ "  } catch(e) { error(e.toString()); }"
				+ "}";
	}

	private static class Engine {
		protected Logger log = LogUtil.getLogger(this);
		private JavascriptEngine<?> jsEngine;
		private ResultHandler resultHandler;

		Engine(String initScript, String graphvisJsLibrary) {

			for (int i = 0; i < engines.length && jsEngine == null; i++) {
				try {
					Class<?> clazz = Class.forName(engines[i]);
					log.debug("Trying Javascript engine [" + engines[i] + "] for Graphviz.");
					JavascriptEngine<?> engine = ((JavascriptEngine<?>) clazz.newInstance());
					ResultHandler handler = new ResultHandler();

					startEngine(engine, handler, initScript, graphvisJsLibrary);

					log.info("Using Javascript engine [" + engines[i] + "] for Graphviz.");
					jsEngine = engine;
					this.resultHandler = handler;
				} catch (Exception e) {
					log.error("Javascript engine [" + engines[i] + "] could not be initialized.", e);
				}
			}

			if (jsEngine == null)
				throw new UnsupportedOperationException("no usable Javascript engines found, tried "+Arrays.toString(engines));
		}

		private void startEngine(JavascriptEngine<?> engine, ResultHandler resultHandler, String initScript, String graphvisJsLibrary) throws JavascriptException {
			log.info("Starting runtime for Javascript Engine...");
			engine.setGlobalAlias("GraphvizJS"); //Set a global alias so all scripts can be cached
			engine.startRuntime();
			log.info("Started Javascript Engine runtime. Initializing Graphviz...");
			engine.executeScript(graphvisJsLibrary);
			engine.executeScript(initScript);
			engine.setResultHandler(resultHandler);
			log.info("Initialized Graphviz");
		}

		public String execute(String call) throws FlowGenerationException {
			try {
				jsEngine.executeScript(call);
				return resultHandler.waitFor();
			} catch (FlowGenerationException e) {
				throw e; //Dont wrap this one!
			} catch (Throwable e) {
				throw new FlowGenerationException(e);
			}
		}

		public void close() {
			jsEngine.closeRuntime();
		}
	}
}
