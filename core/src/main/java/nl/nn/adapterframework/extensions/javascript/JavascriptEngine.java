package nl.nn.adapterframework.extensions.javascript;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public interface JavascriptEngine<E> {
	
	void startRuntime();
	
	void executeScript(String script);
	
	Object executeFunction(String name, Object... parameters);
	
	void closeRuntime();
	
	E get();
	
	public void registerCallback(ISender sender, ParameterResolutionContext prc);
	
}