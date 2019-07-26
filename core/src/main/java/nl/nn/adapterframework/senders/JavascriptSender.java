package nl.nn.adapterframework.senders;

import com.amazonaws.internal.Releasable;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8ScriptCompilationException;
import com.eclipsesource.v8.V8ScriptExecutionException;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;

import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

//Sender used to run javascript code using J2V8

public class JavascriptSender extends SenderWithParametersBase {

	private String fileInput;
	private String inputString;
	private String jsFileName;
	private String jsFunctionName = "main";
	private Object jsResult = "";
	private int numberOfParameters = 0;
	
	//Configure function used to load the JavascriptFile
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(getjsFileName())) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getClassLoader(), getjsFileName());
			} catch (Throwable e) {
				throw new ConfigurationException(
					getLogPrefix() + "got exception searching for [" + getjsFileName() + "]", e);
			}
			if (resource == null) {
				throw new ConfigurationException(
					getLogPrefix() + "cannot find resource [" + getjsFileName() + "]");
			}
			try {
				fileInput = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException(
					getLogPrefix() + "got exception loading [" + getjsFileName() + "]", e);
			}
		}
		if ((StringUtils.isEmpty(fileInput)) && inputString == null) { 
			// No input from file or input string. Only from session-keys?
			throw new ConfigurationException(
				getLogPrefix() + "has neither fileName nor inputString specified");
		}
		if (StringUtils.isEmpty(jsFunctionName)) { 
			// Cannot run the code in factory without any function start point
			throw new ConfigurationException(
				getLogPrefix() + "JavaScript FunctionName not specified!");
		}
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
		
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
		
		//This allows the Javascript function to use a Java function, in this case it is used for System.out.println().
		JavaVoidCallback callback = new JavaVoidCallback() {
			public void invoke(final V8Object receiver, final V8Array parameters) {
				if (parameters.length() > 0) {
					Object arg1 = parameters.get(0);
					System.out.println(arg1);
					if (arg1 instanceof Releasable) {
						((Releasable) arg1).release();
					}
				}
			}
		};
		
		
		try {
			//Start using J2V8
			V8 v8 = V8.createV8Runtime();
			//Javascript can now use print() to use the Java function System.out.println()
			v8.registerJavaMethod(callback, "print");		
			v8.executeScript(fileInput);
			jsResult = v8.executeJSFunction(jsFunctionName, jsParameters);
		} catch (V8ScriptExecutionException e) {
			throw new SenderException(getLogPrefix()+" javascript function does not exist or contains an error", e);
		} catch (V8ScriptCompilationException e) {
	    	throw new SenderException(getLogPrefix()+" invalid javascript syntax given", e);		
		}
        
	    //Pass jsResult, the result of the Javascript function
		return jsResult.toString();
	}

	//The name of the javascript file containing the functions to run
	public void setjsFileName(String jsFileName) {
		this.jsFileName = jsFileName;
	}
	
	public String getjsFileName() {
		return jsFileName;
	}
	
	//The name of the javascript function that will be called (first)
	public void setjsFunctionName(String jsFunctionName) {
		this.jsFunctionName = jsFunctionName;
	}

	public String getjsFunctionName() {
		return jsFunctionName;
	}

}
