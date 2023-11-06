/*
   Copyright 2019-2023 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.extensions.javascript.J2V8;
import nl.nn.adapterframework.extensions.javascript.JavascriptEngine;
import nl.nn.adapterframework.extensions.javascript.JavascriptException;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Sender used to run JavaScript code using J2V8
 *
 * This sender can execute a function of a given javascript file, the result of the function will be the output of the sender.
 * The parameters of the javascript function to run are given as parameters by the adapter configuration
 * The sender doesn't accept nor uses the given input, instead for each argument for the {@link #jsFunctionName} method,
 * you will need to create a parameter on the sender.
 * It is recommended to have the result of the javascript function be of type String, as the output of the sender will be
 * of type String.
 *
 * @author Jarno Huibers
 * @since 7.4
 */

@Category("Advanced")
public class JavascriptSender extends SenderSeries {

	private @Getter String jsFileName;
	private @Getter String jsFunctionName = "main";
	private @Getter JavaScriptEngines engine = JavaScriptEngines.J2V8;

	/** ES6's let/const declaration Pattern. */
	private final Pattern es6VarPattern = Pattern.compile("(?:^|[\\s(;])(let|const)\\s+");

	private String fileInput;


	public enum JavaScriptEngines {
		J2V8(J2V8.class);

		private final Class<? extends JavascriptEngine<?>> engine; //Enum cannot have parameters :(
		private JavaScriptEngines(Class<? extends JavascriptEngine<?>> engine) {
			this.engine = engine;
		}

		public JavascriptEngine<?> create() {
			try {
				return engine.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Javascript engine [" + engine.getSimpleName() + "] could not be initialized.", e);
			}
		}
	}

	@Override
	protected boolean isSenderConfigured() {
		return true;
	}

	//Open function used to load the JavascriptFile
	@Override
	public void open() throws SenderException {
		super.open();

		if (StringUtils.isNotEmpty(getJsFileName())) {
			URL resource = ClassLoaderUtils.getResourceURL(this, getJsFileName());
			if (resource == null) {
				throw new SenderException(getLogPrefix() + "cannot find resource [" + getJsFileName() + "]");
			}
			try {
				fileInput = StreamUtil.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (IOException e) {
				throw new SenderException(getLogPrefix() + "got exception loading [" + getJsFileName() + "]", e);
			}
		}
		if (StringUtils.isEmpty(fileInput)) {
			// No input from file or input string. Only from session-keys?
			throw new SenderException(getLogPrefix() + "has neither fileName nor inputString specified");
		}
		if (StringUtils.isEmpty(jsFunctionName)) {
			// Cannot run the code in factory without any function start point
			throw new SenderException(getLogPrefix() + "JavaScript FunctionName not specified!");
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {

		Object jsResult = "";
		int numberOfParameters = 0;
		JavascriptEngine<?> jsInstance = engine.create();
		try {
			jsInstance.startRuntime();
		} catch (JavascriptException e) {
			throw new SenderException("unable to start Javascript engine", e);
		}

		//Create a Parameter Value List
		ParameterValueList pvl = null;
		try {
			if (getParameterList() != null) {
				pvl = getParameterList().getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + " exception extracting parameters", e);
		}
		if (pvl != null) {
			numberOfParameters = pvl.size();
		}

		//This array will contain the parameters given in the configuration
		Object[] jsParameters = new Object[numberOfParameters];
		for (int i = 0; i < numberOfParameters; i++) {
			ParameterValue pv = pvl.getParameterValue(i);
			Object value = pv.getValue();
			try {
				jsParameters[i] = value instanceof Message ? ((Message) value).asString() : value;
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(), e);
			}
		}

		for (ISender sender : getSenders()) {
			jsInstance.registerCallback(sender, session);
		}

		try {
			//Compile the given Javascript and execute the given Javascript function
			jsInstance.executeScript(adaptES6Literals(fileInput));
			jsResult = jsInstance.executeFunction(jsFunctionName, jsParameters);
		} catch (JavascriptException e) {
			throw new SenderException("unable to execute script/function", e);
		} finally {
			jsInstance.closeRuntime();
		}

		// Pass jsResult, the result of the Javascript function.
		// It is recommended to have the result of the Javascript function be of type String, which will be the output of the sender
		String result = String.valueOf(jsResult);
		if (StringUtils.isEmpty(result) || "null".equals(result) || "undefined".equals(result)) {
			return new SenderResult(Message.nullMessage());
		}
		return new SenderResult(result);
	}

	/**
	 * Since neither engine supports the ES6's "const" or "let" literals. This method adapts the given
	 * helper source written in ES6 to work (by converting let/const to var).
	 *
	 * @param source the helper source.
	 * @return the adapted helper source.
	 **/
	private String adaptES6Literals(final String source) {
		Matcher m = es6VarPattern.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			StringBuilder buf = new StringBuilder(m.group());
			buf.replace(m.start(1) - m.start(), m.end(1) - m.start(), "var");
			m.appendReplacement(sb, buf.toString());
		}
		return m.appendTail(sb).toString();
	}

	/** the name of the javascript file containing the functions to run */
	public void setJsFileName(String jsFileName) {
		this.jsFileName = jsFileName;
	}

	/**
	 * the name of the javascript function that will be called (first)
	 * @ff.default main
	 */
	public void setJsFunctionName(String jsFunctionName) {
		this.jsFunctionName = jsFunctionName;
	}

	/**
	 * the name of the JavaScript engine to be used.
	 * @deprecated Both Nashorn and Rhino are deprecated. Use J2V8 instead.
	 * @ff.default J2V8
	 */
	@Deprecated
	@ConfigurationWarning("JavaScript engines Nashorn and Rhino deprecated. Use \"J2V8\" instead")
	public void setEngineName(JavaScriptEngines engineName) {
		this.engine = engineName;
	}
}
