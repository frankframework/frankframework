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

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public class J2V8 implements JavascriptEngine<V8> {

	private V8 v8;

	public void startRuntime() {
		v8 = V8.createV8Runtime();
	}

	public void startRuntime(String alias, String tempDirectory) {
		v8 = V8.createV8Runtime(alias, tempDirectory);
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

	public V8 getEngine() {
		return v8;
	}

	public void registerCallback(final ISender sender, final ParameterResolutionContext prc) {
		v8.registerJavaMethod(new JavaCallback() {
			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				try {
					String msg = parameters.get(0).toString();
					if(sender instanceof ISenderWithParameters)
						return ((ISenderWithParameters) sender).sendMessage(null, msg, prc);
					else
						return sender.sendMessage(null, msg);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, sender.getName());
	}
}
