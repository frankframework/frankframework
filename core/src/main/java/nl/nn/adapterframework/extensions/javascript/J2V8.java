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

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import org.apache.logging.log4j.LogManager;

public class J2V8 implements JavascriptEngine<V8> {

	private Logger log = LogManager.getLogger(this);
	private V8 v8;

	@Override
	public void startRuntime() {
		startRuntime(null, null);
	}

	/**
	 * If path is null or empty, it will use the log.dir
	 * If the log.dir is relative it will turn it into an absolute path
	 */
	public void startRuntime(String alias, String path) {
		String directory = path;
		if(StringUtils.isEmpty(directory)) {
			directory = AppConstants.getInstance().getResolvedProperty("ibis.tmpdir");
		}
		if(directory != null) {
			File file = new File(directory);
			if (!file.isAbsolute()) {
				String absPath = new File("").getAbsolutePath();
				if(absPath != null) {
					file = new File(absPath, directory);
				}
			}
			String fileDir = file.toString();
			if(StringUtils.isEmpty(fileDir) || !file.isDirectory()) {
				throw new IllegalStateException("unknown or invalid path ["+((StringUtils.isEmpty(fileDir))?"NULL":fileDir)+"], unable to load J2V8 binaries");
			}
			directory = file.getAbsolutePath();
			log.info("resolved J2V8 tempDirectory from path ["+path+"] to directory ["+directory+"]");
		}

		//Directory may be NULL but not empty. The directory has to valid, available and the IBIS must have read+write access to it.
		v8 = V8.createV8Runtime(alias, directory);
	}

	@Override
	public void executeScript(String script) {
		v8.executeScript(script);
	}

	@Override
	public Object executeFunction(String name, Object... parameters) {
		return v8.executeJSFunction(name, parameters);
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
	public void registerCallback(final ISender sender, final IPipeLineSession session) {
		v8.registerJavaMethod(new JavaCallback() {
			@Override
			public Object invoke(V8Object receiver, V8Array parameters) {
				try {
					Message msg = new Message(parameters.get(0));
					return sender.sendMessage(msg, session).asString();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, sender.getName());
	}
}
