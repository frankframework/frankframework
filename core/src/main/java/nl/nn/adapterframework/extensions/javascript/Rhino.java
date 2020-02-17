/*
   Copyright 2019 Integration Partners

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

import org.mozilla.javascript.*;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;

public class Rhino implements JavascriptEngine<Context> {

	private Context cx;
	private Scriptable scope;

	public void startRuntime() {
		cx = Context.enter();
		scope = cx.initStandardObjects();
	}

	public void executeScript(String script) {
		cx.evaluateString(scope, script, "jsScript", 1, null);
	}

	public Object executeFunction(String name, Object... parameters) {
		Function fct = (Function)scope.get(name, scope);
		return fct.call(cx, scope, scope, parameters);
	}

	public void closeRuntime() {
		Context.exit();
	}

	public Context getEngine() {
		return cx;
	}

	@Override
	public void registerCallback(ISender sender, IPipeLineSession session) {
		throw new UnsupportedOperationException("Rhino callback functionality not implemented");
	}
}