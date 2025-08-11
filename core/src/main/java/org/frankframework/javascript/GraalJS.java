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

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.jul.LevelTranslator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.flow.ResultHandler;

/**
 * Javascript engine implementation of GraalJS. If high performance execution of JavaScript code is required, enable the following JVM options:
 * "-XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI" or use the GraalVM Java distribution. Otherwise, the Javascript code is interpreted on every execution.
 * <p/>
 * GraalJS is in Beta phase, so it is not supported by Frank!Framework yet.
 * @since 8.2
 */
@Log4j2
public class GraalJS implements JavascriptEngine<ScriptEngine> {
	private static final Handler LOG_HANDLER = new GraalJsToLog4J2Handler();
	private static final String LANGUAGE_ID = "js";

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
	public void startRuntime() throws JavascriptException {
		log.info("Creating a new GraalJS context");

		try {
			Engine engine = Engine.newBuilder()
					.logHandler(LOG_HANDLER)
					.option("engine.WarnInterpreterOnly", "false")
					.build();

			context = Context.newBuilder(LANGUAGE_ID)
					.logHandler(LOG_HANDLER)
					.allowHostClassLookup(className -> true)
					.allowAllAccess(true)
					.currentWorkingDirectory(TemporaryDirectoryUtils.getTempDirectory("graaljs"))
					.option("js.nashorn-compat", NASHORN_COMPATIBILITY)
					.allowExperimentalOptions(true)
					.engine(engine)
					.build();
		} catch (IOException e) {
			throw new JavascriptException("unable to create temporary directory", e);
		}
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			context.eval(LANGUAGE_ID, script);
		} catch (Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		final Value function;
		try {
			function = context.getBindings(LANGUAGE_ID).getMember(name);
		} catch (Exception e) {
			throw new JavascriptException("unable to find function [" + name + "]", e);
		}

		if (function == null) {
			throw new JavascriptException("unable to find function [" + name + "]");
		}

		try {
			log.debug("executing function [{}]", name);
			return function.execute(parameters);
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
		@HostAccess.Export
		Object apply(Object... arguments);
	}

	@Override
	public void registerCallback(final ISender sender, final PipeLineSession session) {
		if (sender.getName() == null) {
			throw new IllegalStateException("Sender name is required for call backs");
		}
		context.getBindings(LANGUAGE_ID).putMember(sender.getName(), (JavaCallback) s -> {
			try {
				Message msg = Message.asMessage(s[0]);
				try (Message message = sender.sendMessageOrThrow(msg, session)) {
					return message.asString();
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		});
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		// Does not work with GraalJS yet
	}

	private static class GraalJsToLog4J2Handler extends Handler {

		@Override
		public void publish(LogRecord logRecord) {
			final Level log4jLevel = LevelTranslator.toLevel(logRecord.getLevel());
			log.log(log4jLevel, logRecord.getMessage(), logRecord.getThrown());
		}

		@Override
		public void flush() {
			// NO OP
		}

		@Override
		public void close() throws SecurityException {
			// NO OP
		}
	}
}
