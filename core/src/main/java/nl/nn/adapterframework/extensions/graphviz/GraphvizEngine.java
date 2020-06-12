/*
   Copyright 2018-2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.graphviz;

import nl.nn.adapterframework.extensions.javascript.J2V8;
import nl.nn.adapterframework.extensions.javascript.JavascriptEngine;
import nl.nn.adapterframework.extensions.javascript.Nashorn;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;

//TODO: consider moving this to a separate module
/**
 * Javascript V8 engine wrapper for VizJs flow diagrams
 * 
 * @author Niels Meijer
 *
 */
public class GraphvizEngine {
	protected Logger log = LogUtil.getLogger(this);
	private static final ThreadLocal<Env> ENVS = new ThreadLocal<Env>();
	private String graphvizVersion = AppConstants.getInstance().getProperty("graphviz.js.version", "2.0.0");

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
		if(StringUtils.isNotEmpty(graphvizVersion))
			this.graphvizVersion = graphvizVersion;

		//Create the GraphvizEngine, make sure it can find and load the required libraries
		getEnv();
	}

	/**
	 * Execute GraphViz with default options ({@link Format#SVG})
	 * @param src dot file
	 * @return {@link Format#SVG} string
	 * @throws IOException when VizJs files can't be found on the classpath
	 * @throws GraphvizException when some V8 engine error occurs
	 */
	public String execute(String src) throws IOException, GraphvizException {
		return execute(src, Options.create());
	}

	/**
	 * Execute GraphViz
	 * @param src dot file
	 * @param options see {@link Options}
	 * @return string in specified {@link Format}
	 * @throws IOException when VizJs files can't be found on the classpath
	 * @throws GraphvizException when some V8 engine error occurs
	 */
	public String execute(String src, Options options) throws IOException, GraphvizException {
		if(StringUtils.isEmpty(src)) {
			throw new GraphvizException("no dot-file provided");
		}

		long start = 0;
		if(log.isDebugEnabled()) {
			if(log.isTraceEnabled()) log.trace("executing VizJS src["+src+"] options["+options.toString()+"]");
			start = System.currentTimeMillis();
		}

		String call = jsVizExec(src, options);
		String result = getEnv().execute(call);
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
		URL api = ClassUtils.getResourceURL(this.getClass().getClassLoader(), "js/viz-" + version + ".js");
		URL engine = ClassUtils.getResourceURL(this.getClass().getClassLoader(), "js/viz-full.render-" + version + ".js");
		if(api == null || engine == null)
			throw new IOException("failed to open vizjs file for version["+version+"]");
		return Misc.streamToString(api.openStream()) + Misc.streamToString(engine.openStream());
	}


	/**
	 * Creates the GraphvizEngine instance
	 * @throws IOException when the VizJS file can't be found
	 */
	private Env getEnv() throws IOException {
		if(null == ENVS.get()) {
			log.debug("creating new VizJs engine");
			String visJsSource = getVizJsSource(graphvizVersion);
			ENVS.set(new Env(getVisJsWrapper(), visJsSource));
		}
		return ENVS.get();
	}

	/**
	 * Shuts down the GraphvizEngine instance
	 */
	public static void releaseThread() {
		final Env env = ENVS.get();
		if (env != null) {
			env.close();
			ENVS.remove();
		}
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

	private static class Env {
		protected Logger log = LogUtil.getLogger(this);
		private JavascriptEngine<?> jsEngine;
		private ResultHandler resultHandler;

		Env(String initScript, String graphvisJsLibrary) {
			// Available JS Engines. Lower index has priority.
			Class<?>[] engines = new Class[]{J2V8.class, Nashorn.class};

			for (int i = 0; i < engines.length && jsEngine == null; i++) {
				try {
					log.debug("Trying Javascript engine [" + engines[i].getName() + "] for Graphviz.");
					JavascriptEngine<?> engine = ((JavascriptEngine<?>) engines[i].newInstance());
					ResultHandler resultHandler = new ResultHandler();

					startEngine(engine, resultHandler, initScript, graphvisJsLibrary);

					log.info("Using Javascript engine [" + engines[i].getName() + "] for Graphviz.");
					jsEngine = engine;
					this.resultHandler = resultHandler;
				} catch (Exception e) {
					log.error("Javascript engine [" + engines[i].getName() + "] could not be initialized.", e);
				}
			}

			if (jsEngine == null)
				throw new UnsupportedOperationException("Javascript engines could not be initialized.");
		}

		private void startEngine(JavascriptEngine<?> engine, ResultHandler resultHandler, String initScript, String graphvisJsLibrary) throws Exception {
			log.info("Starting runtime for Javascript Engine...");
			engine.setScriptAlias("GraphvizJS"); //Set a global alias so all scripts can be cached
			engine.startRuntime();
			log.info("Started Javascript Engine runtime. Initializing Graphviz...");
			engine.executeScript(graphvisJsLibrary);
			engine.executeScript(initScript);
			engine.setResultHandler(resultHandler);
			log.info("Initialized Graphviz");
		}

		public String execute(String call) throws GraphvizException {
			try {
				jsEngine.executeScript(call);
				return resultHandler.waitFor();
			} catch (Exception e) {
				throw new GraphvizException(e);
			}
		}

		public void close() {
			jsEngine.closeRuntime();
		}
	}
}