/*
   Copyright 2019-2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.javascript.J2V8;
import nl.nn.adapterframework.extensions.javascript.JavascriptEngine;
import nl.nn.adapterframework.extensions.javascript.Rhino;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

/**
 * Sender used to run javascript code using J2V8 or Rhino
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

public class JavascriptSender extends SenderSeries {

	private String fileInput;
	private String inputString;
	private String jsFileName;
	private String jsFunctionName = "main";
	private String engine = "J2V8";

	@Override
	protected boolean isSenderConfigured() {
		return true;
	}

	//Open function used to load the JavascriptFile
	@Override
	public void open() throws SenderException {
		super.open();

		if (StringUtils.isNotEmpty(getJsFileName())) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(this, getJsFileName());
			} catch (Throwable e) {
				throw new SenderException(
					getLogPrefix() + "got exception searching for [" + getJsFileName() + "]", e);
			}
			if (resource == null) {
				throw new SenderException(
					getLogPrefix() + "cannot find resource [" + getJsFileName() + "]");
			}
			try {
				fileInput = Misc.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new SenderException(
					getLogPrefix() + "got exception loading [" + getJsFileName() + "]", e);
			}
		}
		if ((StringUtils.isEmpty(fileInput)) && inputString == null) { 
			// No input from file or input string. Only from session-keys?
			throw new SenderException(
				getLogPrefix() + "has neither fileName nor inputString specified");
		}
		if (StringUtils.isEmpty(jsFunctionName)) { 
			// Cannot run the code in factory without any function start point
			throw new SenderException(
				getLogPrefix() + "JavaScript FunctionName not specified!");
		}
	}

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {

		Object jsResult = "";
		int numberOfParameters = 0;
		JavascriptEngine<?> jsInstance;

		//Create a Parameter Value List
		ParameterValueList pvl=null;
		try {
			if (getParameterList() !=null) {
				pvl=getParameterList().getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+" exception extracting parameters", e);
		}
		if(pvl != null) {
			numberOfParameters = pvl.size();
		}

		//This array will contain the parameters given in the configuration
		Object[] jsParameters = new Object[numberOfParameters];
		for (int i=0; i<numberOfParameters; i++) {
			ParameterValue pv = pvl.getParameterValue(i);
			Object value = pv.getValue();
			try {
				jsParameters[i] = value instanceof Message ? ((Message)value).asString() : value;
			} catch (IOException e) {
				throw new SenderException(getLogPrefix(),e);
			}
		}

		//Start using an engine
		if(engine.equalsIgnoreCase("Rhino")) {
			jsInstance = new Rhino();
			jsInstance.startRuntime();
		} else {
			jsInstance = new J2V8();
			jsInstance.startRuntime();

			for (ISender sender: getSenders()) {
				jsInstance.registerCallback(sender, session);
			} 
		}

		//Compile the given Javascript and execute the given Javascript function
		jsInstance.executeScript(fileInput);
		jsResult = jsInstance.executeFunction(jsFunctionName, jsParameters);

		jsInstance.closeRuntime();

		// Pass jsResult, the result of the Javascript function.
		// It is recommended to have the result of the Javascript function be of type String, which will be the output of the sender
		return new Message(jsResult.toString());
	}

	@IbisDoc({"the name of the javascript file containing the functions to run", ""})
	public void setJsFileName(String jsFileName) {
		this.jsFileName = jsFileName;
	}
	public String getJsFileName() {
		return jsFileName;
	}

	@IbisDoc({"the name of the javascript function that will be called (first)", "main"})
	public void setJsFunctionName(String jsFunctionName) {
		this.jsFunctionName = jsFunctionName;
	}
	public String getJsFunctionName() {
		return jsFunctionName;
	}

	@IbisDoc({"the name of the javascript engine to be used", "J2V8"})
	public void setEngineName(String engineName) {
		this.engine = engineName;
	}
	public String getEngine() {
		return engine;
	}
}
