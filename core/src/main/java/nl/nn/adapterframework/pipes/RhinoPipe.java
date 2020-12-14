/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

/**
 * Rhino JavaScript Runtime Factory Pipe.
 * 
 * This pipe takes all input and pushes it into javascript runtime.
 * The invoke method is called to initialize the runtime
 * Afterward the results are evaluated.
 * <table border="1">
 * <p>
 * <b>Parameters:</b>
 * <tr>
 * <th>name</th>
 * <th>type</th>
 * <th>remarks</th>
 * </tr>
 * <tr>
 * <td><i>any</i></td>
 * <td><i>any</i></td>
 * <td>Any parameters defined on the pipe will be Concatenated into one string and added to input</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Barry Jacobs
 * @deprecated Please use {@link JavascriptSender} instead
 */
@Deprecated
public class RhinoPipe extends FixedForwardPipe {

	private String fileName;
	private String fileInput;
	private String inputString;
	private String paramsInput;
	private String jsfunctionName;
	private String jsfunctionArguments;
	private String sessionKey = null;
	private boolean lookupAtRuntime = false;
	private boolean debug=false;
	/**
	 * Checks for correct configuration, and translates the fileName to a file,
	 * to check existence. If a fileName was specified, the contents of the file
	 * is used as java-script function library. After evaluation the result is returned via
	 * <code>Pipeline</code>.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(getFileName()) && !isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getConfigurationClassLoader(), getFileName());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for [" + getFileName() + "]", e);
			}
			if (resource == null) {
				throw new ConfigurationException("cannot find resource [" + getFileName() + "]");
			}
			try {
				fileInput = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("got exception loading [" + getFileName() + "]", e);
			}
		}
		if ((StringUtils.isEmpty(fileInput)) && inputString == null) { 
			// No input from file or input string. Only from session-keys?
			throw new ConfigurationException("has neither fileName nor inputString specified");
		}
		if (StringUtils.isEmpty(jsfunctionName)) { 
			// Cannot run the code in factory without any function start point
			throw new ConfigurationException("JavaScript functionname not specified!");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		//INIT
		String eol = System.getProperty("line.separator");
		if (message==null || message.isEmpty()) {
			//No input from previous pipes. We will use filename and or string input.
	        if ((StringUtils.isEmpty(fileInput)) && inputString==null && isLookupAtRuntime()) {  // No input from file or input string. Nowhere to GO!
				throw new PipeRunException(this,getLogPrefix(session)+"No input specified anywhere. No string input, no file input and no previous pipe input");
	        }
		}
		
		
		//Default console bindings. Used to map javascript commands to java commands as CONSTANT
		//Console bindings do not work in Rhino. To print from jslib use java.lang.System.out.print("hello world!");
		
		//Get the input from the file at Run Time
		if (StringUtils.isNotEmpty(getFileName()) && isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getConfigurationClassLoader(), getFileName());
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception searching for ["+getFileName()+"]", e);
			}
			if (resource==null) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot find resource ["+getFileName()+"]");
			}
			try {
				fileInput = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception loading ["+getFileName()+"]", e);
			}
		}
		//Get all params as input
		if (getParameterList()!=null) {
			ParameterValueList pvl;
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"exception extracting parameters",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				paramsInput = pv.asStringValue("") + eol + paramsInput ;
			}
		}

		String javascriptcode = "Packages.java;" + eol;
		if (fileInput != null) {
			javascriptcode = javascriptcode + fileInput;
		}
		if (paramsInput != null) {
			javascriptcode = paramsInput + eol + javascriptcode;
		}
		String stringResult = (String) javascriptcode;
		stringResult = "INPUTSTREAM used in case of ERROR" + eol + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + eol + stringResult;
		// Start your engines
		// Rhino engine Ok.
		Context cx = Context.enter();
		Scriptable scope = cx.initStandardObjects();
		if(isDebug()) {
			log.debug("debug active");
			cx.setLanguageVersion(Context.VERSION_1_2);
			cx.setGeneratingDebug(true);
		}
		
		// Load javascript factory with javascript functions from file, stringinput and paraminput
		String jsResult = "";
		try {

			cx.evaluateString(scope, javascriptcode, "jsScript", 1, null);
			Function fct = (Function) scope.get(jsfunctionName, scope);
			// Object result = fct.call(cx, scope, scope, new
			// Object[]{jsfunctionArguments});
			Object result = fct.call(cx, scope, scope, new Object[] { message.asObject() });

			jsResult = (String) Context.jsToJava(result, String.class);
			if (isDebug() && log.isDebugEnabled()) {
				log.debug(getLogPrefix(session)+"jsResult ["+ jsResult+"]");
			}


		} catch (EcmaError ex) {
			throw new PipeRunException(this, "org.mozilla.javascript.EcmaError -> ", ex);
		} finally {
			Context.exit();
		}
		// Use the result
		if ( jsResult != null) {
			stringResult =jsResult;
		}
		if (StringUtils.isEmpty(getSessionKey())) {
			return new PipeRunResult(getForward(), stringResult);
		} else {
			session.put(getSessionKey(), stringResult);
			return new PipeRunResult(getForward(), message);
		}
	}

	@IbisDoc({"when set <code>true</code> or set to something else then \"true\", (even set to the empty string), the debugging is not active", "true"})
	public void setDebug(boolean b) {
		debug = b;
	}

	public boolean isDebug() {
		return debug;
	}

	@IbisDoc({"name of the file containing the Java-script Functions as base input", ""})
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}

	@IbisDoc({"The name of the function in the java-script library to run", ""})
	public void setjsfunctionName(String jsfunctionName) {
		this.jsfunctionName = jsfunctionName;
	}

	public String getjsfunctionName() {
		return jsfunctionName;
	}

	@IbisDoc({"The arguments to run the function in the java-script library to run", ""})
	public void setjsfunctionArguments(String jsfunctionArguments) {
		this.jsfunctionArguments = jsfunctionArguments;
	}

	public String getjsfunctionArguments() {
		return jsfunctionArguments;
	}
	
	@IbisDoc({"when set <code>true</code>, the lookup of the file will be done at runtime instead of at configuration time", "false"})
	public void setLookupAtRuntime(boolean b) {
		lookupAtRuntime = b;
	}

	public boolean isLookupAtRuntime() {
		return lookupAtRuntime;
	}

	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	public String getSessionKey() {
		return sessionKey;
	}
}
