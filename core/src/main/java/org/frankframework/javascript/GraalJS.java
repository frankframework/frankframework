/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.javascript;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import lombok.extern.log4j.Log4j2;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.util.flow.ResultHandler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

/**
 * Javascript engine implementation for GraalJS. If high performance execution of JavaScript code is required, enable the following JVM options:
 * "-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI" or use the GraalVM Java distribution.
 */
@Log4j2
public class GraalJS implements JavascriptEngine<ScriptEngine> {

	private ScriptEngine scriptEngine;
	private Context context = Context.create("js");
	private Bindings bindings;

	private boolean libraryLoaded = false;

	@Override
	public void setGlobalAlias(String alias) {
		// NOOP
	}

	@Override
	public void startRuntime() throws JavascriptException {
		if (!libraryLoaded) {
			scriptEngine = new ScriptEngineManager().getEngineByName("graal.js");
			bindings = scriptEngine.createBindings();
			bindings.put("polyglot.js.allowHostAccess", true); // essential for evaluation on JVM 17
			bindings.put("polyglot.js.allowHostClassLookup", true);
			libraryLoaded = scriptEngine != null;
			if (!libraryLoaded) throw new JavascriptException("error initializing runtime GraalJS engine");
		}
		context = Context.newBuilder()
				.allowHostAccess(HostAccess.SCOPED)
				.allowHostClassLookup(className -> true)
				.engine(Engine.create("js")).build();
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			scriptEngine.eval(script);
		} catch (Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		try {
			Invocable inv = (Invocable) scriptEngine;
			return inv.invokeFunction(name, parameters);
		} catch (Exception e) {
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	@Override
	public void closeRuntime() {
		context.close(true);
		scriptEngine = null;
		libraryLoaded = false;
	}

	@Override
	public ScriptEngine getEngine() {
		return scriptEngine;
	}

	@Override
	public void registerCallback(ISender sender, PipeLineSession session) {
		// Does not work with GraalJS yet
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		// Does not work with GraalJS yet
	}

}
