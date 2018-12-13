/*
   Copyright 2018 Nationale-Nederlanden

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

import org.apache.log4j.Logger;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

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
	private String graphvizVersion = "2.0.0";

	/**
	 * Create a new GraphvizEngine instance. Using version 2.0.0
	 */
	public GraphvizEngine() {
		this(null);
	}

	/**
	 * Create a new GraphvizEngine instance
	 * @param graphvizVersion version of the the VisJs engine to initiate
	 */
	public GraphvizEngine(String graphvizVersion) {
		if(graphvizVersion != null)
			this.graphvizVersion = graphvizVersion;
	}

	/**
	 * Execute GraphViz with default options ({@link Format#SVG})
	 * @param src dot file
	 * @return {@link Format#SVG} string
	 * @throws IOException when VizJs files can't be found on the classpath
	 */
	public String execute(String src) throws IOException {
		return execute(src, Options.create());
	}

	/**
	 * Execute GraphViz
	 * @param src dot file
	 * @param options see {@link Options}
	 * @return string in specified {@link Format}
	 * @throws IOException when VizJs files can't be found on the classpath
	 */
	public String execute(String src, Options options) throws IOException {
		long start = 0;
		if(log.isDebugEnabled()) {
			log.debug("executing VizJS src["+src+"] options["+options.toString()+"]");
			start = System.currentTimeMillis();
		}

		final Env env = ENVS.get();
		if (env == null) {
			log.debug("creating new VizJs engine");
			String visJsSource = getVizJsSource(graphvizVersion);
			ENVS.set(new Env(getVisJsWrapper(), visJsSource));
		}

		String call = jsVizExec(src, options);
		String result = ENVS.get().execute(call);
		if(start > 0) {
			log.debug("executed VisJs in ["+(System.currentTimeMillis() - start)+"]ms");
		}
		return options.postProcess(result);
	}

	private String jsVizExec(String src, Options options) {
		return src.startsWith("render") ? src : ("render('" + jsEscape(src) + "'," + options.toJson(false) + ");");
	}

	private String jsEscape(String js) {
		return js.replaceAll("\r\n", " ").replace("\\", "\\\\").replace("'", "\\'");
	}

	private String getVizJsSource(String version) throws IOException {
		URL api = ClassUtils.getResourceURL(this, "js/viz-" + version + ".js");
		URL engine = ClassUtils.getResourceURL(this, "js/viz-full.render-" + version + ".js");
		if(api == null || engine == null)
			throw new IOException("failed to open vizjs file for version["+version+"]");
		return Misc.streamToString(api.openStream()) + Misc.streamToString(engine.openStream());
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

		final V8 v8;
		final ResultHandler resultHandler = new ResultHandler();

		Env(String init, String viz) {
			log.info("starting V8 runtime...");
			v8 = V8.createV8Runtime();
			log.info("started V8 runtime. Initializing graphviz...");
			v8.executeVoidScript(viz);
			v8.executeVoidScript(init);
			v8.registerJavaMethod(new JavaVoidCallback() {
				@Override
				public void invoke(V8Object receiver, V8Array parameters) {
					resultHandler.setResult(parameters.getString(0));
				}
			}, "result");
			v8.registerJavaMethod(new JavaVoidCallback() {
				@Override
				public void invoke(V8Object receiver, V8Array parameters) {
					resultHandler.setError(parameters.getString(0));
				}
			}, "error");
			log.info("initialized graphviz");
		}

		public String execute(String call) {
			try {
				v8.executeVoidScript(call);
				return resultHandler.waitFor();
			} catch (Exception e) {
				log.error("unable to successfully execute graphviz");
				return null;
			}
		}

		public void close() {
			v8.release(true);
		}
	}
}