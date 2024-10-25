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
package org.frankframework.senders;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.Mandatory;
import org.frankframework.doc.Optional;
import org.frankframework.javascript.GraalJS;
import org.frankframework.javascript.J2V8;
import org.frankframework.javascript.JavascriptEngine;
import org.frankframework.javascript.JavascriptException;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;

/**
 * Sender used to run JavaScript code using `JavascriptEngine` implementations.
 * <p>
 * This sender can execute a function of a given Javascript file, the result of the function will be the output of the sender.
 * The parameters of the Javascript function to run are given as parameters by the adapter configuration
 * The sender doesn't accept nor uses the given input, instead for each argument for the {@link #jsFunctionName} method,
 * you will need to create a parameter on the sender.
 * </p>
 * <p>
 * The result of the Javascript function should be of type String, or directly convertible to String from a primitive type
 * or an array of primitive types / strings, as the output of the sender will be of type String.
 * </p>
 * <p>
 * Failure to ensure the output is a string may mean the result will look like {@code [Object object]}.
 * </p>
 *
 * @since 7.4
 */
@Category(Category.Type.ADVANCED)
public class JavascriptSender extends SenderSeries {

	private @Getter String jsFileName;
	private @Getter String jsFunctionName = "main";
	private @Getter JavaScriptEngines engine = JavaScriptEngines.J2V8;

	/** ES6's let/const declaration Pattern. */
	private final Pattern es6VarPattern = Pattern.compile("(?:^|[\\s(;])(let|const)\\s+");

	private String javascriptFileContents;


	public enum JavaScriptEngines {
		J2V8(J2V8.class),
		GRAALJS(GraalJS.class);

		private final Class<? extends JavascriptEngine<?>> engine; //Enum cannot have parameters :(
		JavaScriptEngines(Class<? extends JavascriptEngine<?>> engine) {
			this.engine = engine;
		}

		public JavascriptEngine<?> create() {
			try {
				return ClassUtils.newInstance(engine);
			} catch (Exception e) {
				throw new IllegalStateException("Javascript engine [" + engine.getSimpleName() + "] could not be initialized.", e);
			}
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(jsFileName)) {
			throw new ConfigurationException("no jsFileName specified");
		}
		if (StringUtils.isEmpty(jsFunctionName)) {
			// Cannot run the code in factory without any function start point
			throw new ConfigurationException("JavaScript FunctionName not specified!");
		}

		super.configure();

		URL resource = ClassLoaderUtils.getResourceURL(this, getJsFileName());
		if (resource == null) {
			throw new ConfigurationException("cannot find resource [" + getJsFileName() + "]");
		}
		try {
			String fileInput = StreamUtil.resourceToString(resource, Misc.LINE_SEPARATOR);
			javascriptFileContents = adaptES6Literals(fileInput);
		} catch (IOException e) {
			throw new ConfigurationException("got exception reading [" + getJsFileName() + "]", e);
		}
	}

	@Override
	protected boolean isSenderConfigured() {
		return true;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
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
			throw new SenderException("unable to extract parameters", e);
		}
		int numberOfParameters = 0;
		if (pvl != null) {
			numberOfParameters = pvl.size();
		}

		//This array will contain the parameters given in the configuration
		Object[] jsParameters = new Object[numberOfParameters];
		for (int i = 0; i < numberOfParameters; i++) {
			ParameterValue pv = pvl.getParameterValue(i);
			Object value = pv.getValue();
			try {
				jsParameters[i] = value instanceof Message m ? m.asString() : value;
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(), e);
			}
		}

		for (ISender sender : getSenders()) {
			jsInstance.registerCallback(sender, session);
		}

		String result;
		try {
			//Compile the given Javascript and execute the given Javascript function
			jsInstance.executeScript(javascriptFileContents);
			Object jsResult = jsInstance.executeFunction(jsFunctionName, jsParameters);
			result = String.valueOf(jsResult);
		} catch (JavascriptException e) {
			throw new SenderException("unable to execute script/function", e);
		} finally {
			jsInstance.closeRuntime();
		}

		// Pass jsResult, the result of the Javascript function.
		// It is recommended to have the result of the Javascript function be of type String, which will be the output of the sender
		if (StringUtils.isEmpty(result) || "null".equals(result) || "undefined".equals(result)) {
			return new SenderResult(Message.nullMessage());
		}
		return new SenderResult(result);
	}

	@Optional
	@Override
	public void addSender(ISender sender) {
		super.addSender(sender);
	}

	/**
	 * J2V8 engine does not support the ES6's "const" or "let" literals. This method adapts the given
	 * helper source written in ES6 to work (by converting let/const to var).
	 *
	 * @param source the helper source.
	 * @return the adapted helper source.
	 **/
	private String adaptES6Literals(final String source) {
		if (engine != JavaScriptEngines.J2V8) {
			return source;
		}
		Matcher m = es6VarPattern.matcher(source);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			StringBuilder buf = new StringBuilder(m.group());
			buf.replace(m.start(1) - m.start(), m.end(1) - m.start(), "var");
			m.appendReplacement(sb, buf.toString());
		}
		return m.appendTail(sb).toString();
	}

	/** the name of the javascript file containing the functions to run */
	@Mandatory
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
	 * the name of the JavaScript engine to use.
	 * @ff.default J2V8
	 */
	public void setEngineName(JavaScriptEngines engineName) {
		this.engine = engineName;
	}
}
