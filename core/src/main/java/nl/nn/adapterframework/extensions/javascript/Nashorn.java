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
package nl.nn.adapterframework.extensions.javascript;

import java.net.URL;
import java.util.function.Consumer;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.flow.ResultHandler;

public class Nashorn implements JavascriptEngine<ScriptEngine> {

	private ScriptEngine engine;
	private String alias;
	private ScriptContext localEngineScope;

	@Override
	public void setGlobalAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void startRuntime() throws JavascriptException {
		ScriptEngineManager engineManager = new ScriptEngineManager();
		if (StringUtils.isNotEmpty(alias)) {
			engineManager.put(alias, "this"); // Register alias as 'this' in the Global Scope.
		}

		try {
			engine = engineManager.getEngineByName("nashorn");
			localEngineScope = engine.getContext();

			//Add PromiseJS polyfill
			URL promise = ClassLoaderUtils.getResourceURL("net/arnx/nashorn/lib/promise.js");
			executeScript(StreamUtil.resourceToString(promise));
		} catch (Exception e) { //Catch all exceptions
			throw new JavascriptException("error initializing Nashorn, unable to load Promise.js", e);
		}
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			engine.eval(script, localEngineScope);
		} catch (Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		try {
			return ((Invocable) engine).invokeFunction(name, parameters);
		} catch (Exception e) {
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	@Override
	public void closeRuntime() {
		//Nothing to close :)
	}

	@Override
	public ScriptEngine getEngine() {
		return engine;
	}

	@Override
	public void registerCallback(ISender sender, PipeLineSession session) {
		ThrowingFunction<String, String, JavascriptException> method = (param) -> {
			try {
				Message msg = Message.asMessage(param);
				return sender.sendMessageOrThrow(msg, session).asString();
			} catch (Exception e) {
				throw new JavascriptException(e);
			}
		};
		getEngine().put(sender.getName(), method);
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		getEngine().put("result", (Consumer<String>) resultHandler::setResult);
		getEngine().put("error", (Consumer<String>) resultHandler::setError);
	}
}
