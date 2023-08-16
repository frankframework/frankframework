/*
   Copyright 2019-2021 WeAreFrank!

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.flow.ResultHandler;

public class Rhino implements JavascriptEngine<Context> {

	private Context cx;
	private Scriptable scope;
	private String alias = "jsScript";

	@Override
	public void setGlobalAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public void startRuntime() {
		cx = Context.enter();
		scope = cx.initStandardObjects();
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			cx.evaluateString(scope, script, alias, 1, null);
		} catch (Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		try {
			Function fct = (Function)scope.get(name, scope);
			return fct.call(cx, scope, scope, parameters);
		} catch (Exception e) {
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	@Override
	public void closeRuntime() {
		Context.exit();
	}

	@Override
	public Context getEngine() {
		return cx;
	}

	@Override
	public void registerCallback(ISender sender, PipeLineSession session) {
		throw new UnsupportedOperationException("Rhino callback functionality not implemented");
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		throw new UnsupportedOperationException("Rhino callback functionality not implemented");
	}
}