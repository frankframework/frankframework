/*
   Copyright 2018-2019 Nationale-Nederlanden

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

import java.io.IOException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import nl.nn.adapterframework.extensions.javascript.J2V8;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

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
			log.debug("executing VizJS src["+src+"] options["+options.toString()+"]");
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
		URL api = ClassUtils.getResourceURL(this, "js/viz-" + version + ".js");
		URL engine = ClassUtils.getResourceURL(this, "js/viz-full.render-" + version + ".js");
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
			String tempDir = AppConstants.getInstance().getString("log.dir", null);
			if(tempDir != null && tempDir.isEmpty()) //Make sure to not pass an empty directory
				tempDir = null;
			ENVS.set(new Env(getVisJsWrapper(), visJsSource, "GraphvizJS", tempDir));
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

		J2V8 V8Instance = new J2V8();
		final ResultHandler resultHandler = new ResultHandler();

		/**
		 * It's important to register the JS scripts under the same alias so it can be cached
		 * Use the log.dir to extract the SO/DLL files into, make sure this is using an absolute path and not a relative one!!
		 */
		Env(String initScript, String graphvisJsLibrary, String alias, String tempDirectory) {
			log.info("starting V8 runtime...");
			V8Instance.startRuntime(alias, tempDirectory);
			log.info("started V8 runtime. Initializing graphviz...");
			V8Instance.executeScript(graphvisJsLibrary);
			V8Instance.executeScript(initScript);
			V8Instance.getEngine().registerJavaMethod(new JavaVoidCallback() {
				@Override
				public void invoke(V8Object receiver, V8Array parameters) {
					resultHandler.setResult(parameters.getString(0));
				}
			}, "result");
			V8Instance.getEngine().registerJavaMethod(new JavaVoidCallback() {
				@Override
				public void invoke(V8Object receiver, V8Array parameters) {
					resultHandler.setError(parameters.getString(0));
				}
			}, "error");
			log.info("initialized graphviz");
		}

		public String execute(String call) throws GraphvizException {
			try {
				V8Instance.executeScript(call);
				return resultHandler.waitFor();
			} catch (Exception e) {
				throw new GraphvizException(e);
			}
		}

		public void close() {
			V8Instance.closeRuntime();
		}
	}
}