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
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import lombok.extern.log4j.Log4j2;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.flow.ResultHandler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

/**
 * Javascript engine implementation of GraalJS. If high performance execution of JavaScript code is required, enable the following JVM options:
 * "-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI" or use the GraalVM Java distribution. Otherwise, the Javascript code is interpreted on every execution.
 * <p/>
 * GraalJS is in Beta phase, so it is not supported by Frank!Framework yet.
 * @since 8.2
 */
@Log4j2
@Deprecated
public class GraalJS implements JavascriptEngine<ScriptEngine> {

	// Fixes some return types and other compatibility issues, but GraphvizJS still does not work. Warning: has security disadvantages and puts ECMAScript 5 compatibility on!
	private static final String NASHORN_COMPATIBILITY = AppConstants.getInstance().getString("javascript.graaljs.nashorn-compat", "false");

	private ScriptEngine scriptEngine; // Please avoid usage; preferred is through the 'context'
	private Context context;
	private boolean libraryLoaded = false;

	@Override
	public void setGlobalAlias(String alias) {
		// Not supported by GraalJS
	}

	@Override
	public void startRuntime() {
		log.info("Creating a new GraalJS context");

		context = Context.newBuilder()
				.allowHostClassLookup(className -> true)
				.allowAllAccess(true)
				.currentWorkingDirectory(TemporaryDirectoryUtils.getTempDirectory())
				.option("js.nashorn-compat", NASHORN_COMPATIBILITY)
				.allowExperimentalOptions(true)
				.engine(Engine.create("js")).build();
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			context.eval("js", script);
		} catch (Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		try {
			String joinedParameters = joinParameters(parameters);
			return context.eval("js", name + "(" + joinedParameters + ");");
		} catch (Exception e) {
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	private static String joinParameters(Object[] parameters) {
		if (parameters.length == 0) {
			return "";
		}
		StringBuilder parameterBuilder = new StringBuilder();
		for (int i = 0; i < parameters.length; i++) {
			Object parameter = parameters[i];
			if (parameter instanceof String) { // Fixes Strings with quotes or spaces
				parameterBuilder.append("'").append(parameter).append("'");
			} else {
				parameterBuilder.append(parameter);
			}
			if (i != parameters.length - 1) {
				parameterBuilder.append(",");
			}
		}
		return parameterBuilder.toString();
	}

	@Override
	public void closeRuntime() {
		context.close(true);
		scriptEngine = null;
		libraryLoaded = false;
	}

	@Override
	public ScriptEngine getEngine() {
		if (!libraryLoaded) {
			scriptEngine = new ScriptEngineManager().getEngineByName("graal.js");
			Bindings bindings = scriptEngine.createBindings();
			bindings.put("polyglot.js.allowHostAccess", true);
			bindings.put("polyglot.js.allowHostClassLookup", true);
			libraryLoaded = scriptEngine != null;
		}
		return scriptEngine;
	}

	@FunctionalInterface
	public interface JavaCallback {
		Object apply(Object... arguments);
	}

	@Override
	public void registerCallback(final ISender sender, final PipeLineSession session) {
		if (sender.getName() == null) {
			throw new IllegalStateException("Sender name is required for call backs");
		}
		context.getBindings("js").putMember(sender.getName(), (JavaCallback) s -> {
			try {
				Message msg = Message.asMessage(s[0]);
				try (Message message = sender.sendMessageOrThrow(msg, session)) {
					return message.asString();
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		// Does not work with GraalJS yet
	}

}
