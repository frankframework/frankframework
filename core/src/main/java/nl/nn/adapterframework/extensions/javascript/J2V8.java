/*
   Copyright 2019-2022 WeAreFrank!

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

import java.io.File;
import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.flow.ResultHandler;

public class J2V8 implements JavascriptEngine<V8> {

	private Logger log = LogUtil.getLogger(this);
	private V8 v8;
	private String alias = null;

	private static boolean j2v8LibraryLoaded = false;
	private static Object j2v8Lock = new Object();

	@Override
	public void setGlobalAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * Use the ${ibis.tmpdir} to extract the SO/DLL files into.
	 * If the ${ibis.tmpdir} is relative it will turn it into an absolute path
	 */
	private String getTempDirectory() {
		String directory = AppConstants.getInstance().getResolvedProperty("ibis.tmpdir");

		if (StringUtils.isNotEmpty(directory)) {
			File file = new File(directory);
			if (!file.isAbsolute()) {
				String absPath = new File("").getAbsolutePath();
				if(absPath != null) {
					file = new File(absPath, directory);
				}
			}
			if(!file.exists()) {
				file.mkdirs();
			}
			String fileDir = file.getPath();
			if(StringUtils.isEmpty(fileDir) || !file.isDirectory()) {
				throw new IllegalStateException("unknown or invalid path ["+((StringUtils.isEmpty(fileDir))?"NULL":fileDir)+"], unable to load J2V8 binaries");
			}
			directory = file.getAbsolutePath();
		}
		log.info("resolved J2V8 tempDirectory to directory [" + directory + "]");

		//Directory may be NULL but not empty. The directory has to valid, available and the IBIS must have read+write access to it.
		return StringUtils.isEmpty(directory) ? null : directory;
	}

	@Override
	public void startRuntime() throws JavascriptException {
		// The V8 runtime (DLL/SO files) have to be extracted somewhere, using an absolute path. Defaults to ${ibis.tmpdir}
		String tempDirectory = getTempDirectory();
		// preload the library to avoid having to set ALL FILES execute permission
		if (!j2v8LibraryLoaded) {
			synchronized (j2v8Lock) {
				if (!j2v8LibraryLoaded) {
					FrankJ2V8LibraryLoader.loadLibrary(tempDirectory);
					// now update the private boolean field in the ancestor that indicates that the library has been loaded.
					try {
						Field privateField = V8.class.getDeclaredField("nativeLibraryLoaded");
						privateField.setAccessible(true); // it additional permissions might be required for this
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
			return v8.executeJSFunction(name, parameters);
		} catch (Exception e) {
			throw new JavascriptException("error executing function [" + name + "]", e);
		}
	}

	@Override
	public void closeRuntime() {
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
					return sender.sendMessageOrThrow(msg, session).asString();
				} catch (Exception e) {
					throw new RuntimeException(e);
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
