package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.J2V8;
import nl.nn.adapterframework.pipes.JavascriptEngine;
import nl.nn.adapterframework.pipes.Rhino;

import java.net.URL;
import java.util.Iterator;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * Sender used to run javascript code using J2V8 or Rhino
 * 
 * This sender can execute a function of a given javascript file, the result of the function will be the output of the sender.
 * The parameters of the javascript function to run are given as parameters by the adapter configuration
 * The input of the sender can only be used if it is given as a parameter using the originalMessage SessionKey.
 * It is recommended to have the result of the javascript function be of type String, as the output of the sender will be 
 * of type String.
 * 
 * @author Jarno Huibers
 * @since 7.3
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
	public void open() throws SenderException {
		super.open();

		if (StringUtils.isNotEmpty(getjsFileName())) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getClassLoader(), getjsFileName());
			} catch (Throwable e) {
				throw new SenderException(
					getLogPrefix() + "got exception searching for [" + getjsFileName() + "]", e);
			}
			if (resource == null) {
				throw new SenderException(
					getLogPrefix() + "cannot find resource [" + getjsFileName() + "]");
			}
			try {
				fileInput = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new SenderException(
					getLogPrefix() + "got exception loading [" + getjsFileName() + "]", e);
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

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
				
		Object jsResult = "";
		int numberOfParameters = 0;
		JavascriptEngine jsInstance;
		
		//Create a Parameter Value List
		ParameterValueList pvl;
		try {
			pvl = prc.getValues(getParameterList());
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+" exception extracting parameters", e);
		}
		if(pvl != null)
			numberOfParameters = pvl.size();
		//This array will contain the parameters given in the configuration
		Object[] jsParameters = new Object[numberOfParameters];
		for (int i=0; i<numberOfParameters; i++) {
			ParameterValue pv = pvl.getParameterValue(i);
			jsParameters[i] = pv.getValue();
		}
		
		//Start using an engine
		if(engine.equalsIgnoreCase("Rhino")) {
			jsInstance = new Rhino();
			jsInstance.startRuntime();
		}
		else {
			jsInstance = new J2V8();
			jsInstance.startRuntime();
			for (Iterator<ISender> iterator = getSenderIterator(); iterator.hasNext();) {
				ISender sender = iterator.next();
				jsInstance.registerCallback(sender, prc);
			} 
		}
		
		//Compile the given Javascript and execute the given Javascript function
		jsInstance.executeScript(fileInput);	
		jsResult = jsInstance.executeFunction(jsFunctionName, jsParameters);
		
		jsInstance.closeRuntime();
        
	    /*Pass jsResult, the result of the Javascript function.
		It is recommended to have the result of the Javascript function be of type String, which will be the output of the sender */
		return jsResult.toString();
	}

	@IbisDoc({"the name of the javascript file containing the functions to run", ""})
	public void setjsFileName(String jsFileName) {
		this.jsFileName = jsFileName;
	}
	
	public String getjsFileName() {
		return jsFileName;
	}
	
	@IbisDoc({"the name of the javascript function that will be called (first)", "main"})
	public void setjsFunctionName(String jsFunctionName) {
		this.jsFunctionName = jsFunctionName;
	}

	public String getjsFunctionName() {
		return jsFunctionName;
	}
	
	@IbisDoc({"the name of the javascript engine to be used", "J2V8"})
	public void setengineName(String engineName) {
		this.engine = engineName;
	}
	
	public String getEngine() {
		return engine;
	}

}
