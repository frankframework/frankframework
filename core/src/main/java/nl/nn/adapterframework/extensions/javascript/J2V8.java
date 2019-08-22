package nl.nn.adapterframework.extensions.javascript;

import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.AppConstants;

public class J2V8 implements JavascriptEngine<V8> {

	private V8 v8;
	
	public void startRuntime() {
		v8 = V8.createV8Runtime();
	}
	
	public void executeScript(String script) {
		v8.executeScript(script);
	}
	
	public Object executeFunction(String name, Object... parameters) {
		return v8.executeJSFunction(name, parameters);
	}
	
	public void closeRuntime() {
		v8.release(true);
	}
	
	public V8 get() {
		return v8;
	}

	public void registerCallback(final ISender sender, final ParameterResolutionContext prc) {
		v8.registerJavaMethod(new JavaVoidCallback() {
			@Override
			public void invoke(V8Object receiver, V8Array parameters) {
				try {
					if(sender instanceof ISenderWithParameters)
						((ISenderWithParameters) sender).sendMessage(null, parameters.get(0).toString(), prc);
					else
						sender.sendMessage(null, parameters.get(0).toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, sender.getName());
	}

}
