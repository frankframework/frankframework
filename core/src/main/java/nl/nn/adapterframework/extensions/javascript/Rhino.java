package nl.nn.adapterframework.extensions.javascript;

import org.mozilla.javascript.*;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

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
	
	public Context get() {
		return cx;
	}

	@Override
	public void registerCallback(ISender sender, ParameterResolutionContext prc) {
		// TODO Auto-generated method stub
		
	}

}