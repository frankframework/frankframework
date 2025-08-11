/*
   Copyright 2019-2024 WeAreFrank!

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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.flow.ResultHandler;

public class J2V8 implements JavascriptEngine<V8> {

	private V8 v8;
	private String alias = null;
	private final List<V8Value> objectsToRelease = new ArrayList<>();

	private static boolean j2v8LibraryLoaded = false;
	private static final Object j2v8Lock = new Object();

	@Override
	public void setGlobalAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * The V8 runtime (DLL/SO files) have to be extracted somewhere, using an absolute path.
	 * Defaults to ${ibis.tmpdir}
	 */
	@Override
	public void startRuntime() throws JavascriptException {
		String tempDirectory = TemporaryDirectoryUtils.getTempDirectory().toString();

		// preload the library to avoid having to set ALL FILES execute permission
		if (!j2v8LibraryLoaded) {
			synchronized (j2v8Lock) {
				if (!j2v8LibraryLoaded) {
					FrankJ2V8LibraryLoader.loadLibrary(tempDirectory);
					// now update the private boolean field in the ancestor that indicates that the library has been loaded.
					try {
						Field privateField = V8.class.getDeclaredField("nativeLibraryLoaded");
						privateField.setAccessible(true); // additional permissions might be required for this
						privateField.set(null, true);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						throw new JavascriptException("Cannot indicate that native J2V8 library has been loaded", e);
					}
					j2v8LibraryLoaded = true;
				}
			}
		}
		v8 = V8.createV8Runtime(alias, tempDirectory);
	}

	@Override
	public void executeScript(String script) throws JavascriptException {
		try {
			v8.executeScript(script);
		} catch(Exception e) {
			throw new JavascriptException("error executing script", e);
		}
	}

	@Override
	public Object executeFunction(String name, Object... parameters) throws JavascriptException {
		try {
			Object result = v8.executeJSFunction(name, parameters);
			if (result instanceof V8Value v8Value) {
				objectsToRelease.add(v8Value);
			}
			return result;
		} catch (Exception e) {
			if ("undefined:0: TypeError: undefined is not a function".equals(e.getMessage())) {
				throw new JavascriptException("unable to find function [" + name + "]");
			}
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	@Override
	public void closeRuntime() {
		objectsToRelease.forEach(V8Value::release);
		objectsToRelease.clear();
		v8.release(true);
	}

	@Override
	public V8 getEngine() {
		return v8;
	}

	@Override
	public void registerCallback(final ISender sender, final PipeLineSession session) {
		v8.registerJavaMethod(new JavaCallback() {
			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				try {
					Message msg = Message.asMessage(parameters.get(0));
					try (Message message = sender.sendMessageOrThrow(msg, session)) {
						return message.asString();
					}
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
		}, sender.getName());
	}

	@Override
	public void setResultHandler(ResultHandler resultHandler) {
		getEngine().registerJavaMethod(new JavaVoidCallback() {
			@Override
			public void invoke(V8Object receiver, V8Array parameters) {
				resultHandler.setResult(parameters.getString(0));
			}
		}, "result");
		getEngine().registerJavaMethod(new JavaVoidCallback() {
			@Override
			public void invoke(V8Object receiver, V8Array parameters) {
				resultHandler.setError(parameters.getString(0));
			}
		}, "error");
	}
}
