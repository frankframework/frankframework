/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.Reader;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Provides an example of a pipe. It may return the contents of a file
 * (in the classpath) when <code>fileName</code> or <code>fileNameSessionKey</code> is specified, otherwise the
 * input of <code>returnString</code> is returned.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr>
 *   <td><i>any</i></td><td><i>any</i></td>
 * 	 <td>Any parameters defined on the pipe will be used for replacements. Each occurrence
 * 		 of <code>${name-of-parameter}</code> in the file {@link #setFileName(String) fileName} 
 *       will be replaced by its corresponding <i>value-of-parameter</i>. <br>
 *       This works only with files, not with values supplied in attribute {@link #setReturnString(String) returnString}</td>
 * </tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td>"filenotfound"</td><td>file not found (when this forward isn't specified an exception will be thrown)</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 */
public class FixedResult extends FixedForwardPipe {
	
	private final static String FILE_NOT_FOUND_FORWARD = "filenotfound";
	
	AppConstants appConstants;
    private String fileName;
    private String fileNameSessionKey;
    private String returnString;
    private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;
	private boolean lookupAtRuntime=false;
	private boolean replaceFixedParams=false;
	
    /**
     * checks for correct configuration, and translates the fileName to
     * a file, to check existence. 
     * If a fileName or fileNameSessionKey was specified, the contents of the file is put in the
     * <code>returnString</code>, so that the <code>returnString</code>
     * may always be returned.
     * @throws ConfigurationException
     */
    @Override
	public void configure() throws ConfigurationException {
		super.configure();
		appConstants = AppConstants.getInstance(getConfigurationClassLoader());
		if (StringUtils.isNotEmpty(getFileName()) && !isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getConfigurationClassLoader(), getFileName());
			} catch (Throwable e) {
				throw new ConfigurationException(getLogPrefix(null)+"got exception searching for ["+getFileName()+"]", e);
			}
			if (resource==null) {
				throw new ConfigurationException(getLogPrefix(null)+"cannot find resource ["+getFileName()+"]");
			}
            try {
				returnString = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
            } catch (Throwable e) {
                throw new ConfigurationException(getLogPrefix(null)+"got exception loading ["+getFileName()+"]", e);
            }
        }
        if ((StringUtils.isEmpty(fileName)) && (StringUtils.isEmpty(fileNameSessionKey)) && returnString==null) {  // allow an empty returnString to be specified
            throw new ConfigurationException(getLogPrefix(null)+"has neither fileName nor fileNameSessionKey nor returnString specified");
        }
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
    }
    
	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		String result=returnString;
		String fileName = null;
		if (StringUtils.isNotEmpty(getFileNameSessionKey())) {
			fileName = (String)session.get(fileNameSessionKey);
		}
		if (fileName == null && StringUtils.isNotEmpty(getFileName()) && isLookupAtRuntime()) {
			fileName = getFileName();
		}
		if (StringUtils.isNotEmpty(fileName)) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(getConfigurationClassLoader(), fileName);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception searching for ["+fileName+"]", e);
			}
			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				} else {
					throw new PipeRunException(this,getLogPrefix(session)+"cannot find resource ["+fileName+"]");
				}
			}
			try {
				result = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception loading ["+fileName+"]", e);
			}
		}
		if (getParameterList()!=null) {
			ParameterValueList pvl;
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"exception extracting parameters",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				String replaceFrom;
				if (isReplaceFixedParams()) {
					replaceFrom=pv.getDefinition().getName();
				} else {
					replaceFrom="${"+pv.getDefinition().getName()+"}";
				}
				result=replace(result,replaceFrom,pv.asStringValue(""));
			}
		}

		try (Reader dummy = message.asReader()) {
			// get the inputstream and close it, to avoid connection leaking when the message itself is not consumed
		} catch (IOException e) {
			log.warn("Exception reading ignored inputstream", e);
		}
		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, session, appConstants);
		}

		if (StringUtils.isNotEmpty(styleSheetName)) {
			URL xsltSource = ClassUtils.getResourceURL(getConfigurationClassLoader(), styleSheetName);
			if (xsltSource!=null) {
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(xsltSource);
					xsltResult = XmlUtils.transformXml(transformer, result);
					result = xsltResult;
				} catch (IOException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"cannot retrieve ["+ styleSheetName + "], resource [" + xsltSource.toString() + "]", e);
				} catch (SAXException|TransformerConfigurationException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error creating transformer from file [" + styleSheetName + "]", e);
				} catch (TransformerException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", e);
				}
			}
		}

	    log.debug(getLogPrefix(session)+ " returning fixed result [" + result + "]");

   		return new PipeRunResult(getForward(), result);
	}

	public static String replace (String target, String from, String to) {   
		// target is the original string
		// from   is the string to be replaced
		// to     is the string which will used to replace
		int start = target.indexOf (from);
		if (start==-1) return target;
		int lf = from.length();
		char [] targetChars = target.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int copyFrom=0;
		while (start != -1) {
			buffer.append (targetChars, copyFrom, start-copyFrom);
			buffer.append (to);
			copyFrom=start+lf;
			start = target.indexOf (from, copyFrom);
		}
		buffer.append (targetChars, copyFrom, targetChars.length-copyFrom);
		return buffer.toString();
	}

	@IbisDoc({"should values between ${ and } be resolved from the pipelinesession (search order: 1) system properties 2) pipelinesession variables 3) application properties)", "false"})
	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}
	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

    /**
     * Sets the name of the filename. The fileName should not be specified
     * as an absolute path, but as a resource in the classpath.
     */
	@IbisDoc({"name of the file containing the resultmessage", ""})
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
	public String getFileName() {
		return fileName;
	}
	
	/**
	 * @param filenameSessionKey the session key that contains the name of the file
	 */
	@IbisDoc({"name of the session key containing the file name of the file containing the result message", ""})
	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	@IbisDoc({"returned message", ""})
    public void setReturnString(String returnString) {
        this.returnString = returnString;
    }
	public String getReturnString() {
		return returnString;
	}

	public String getReplaceFrom() {
		return replaceFrom;
	}
	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public String getReplaceTo() {
		return replaceTo;
	}
	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public String getStyleSheetName() {
		return styleSheetName;
	}
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

	@IbisDoc({"when set <code>true</code>, the lookup of the file will be done at runtime instead of at configuration time", "false"})
	public void setLookupAtRuntime(boolean b){
		lookupAtRuntime=b;
	}
	public boolean isLookupAtRuntime(){
		return lookupAtRuntime;
	}

	@IbisDoc({"when set <code>true</code>, any parameter is used for replacements but with <code>name-of-parameter</code> and not <code>${name-of-parameter}</code>", "false"})
	public void setReplaceFixedParams(boolean b){
		replaceFixedParams=b;
	}
	public boolean isReplaceFixedParams(){
		return replaceFixedParams;
	}

}
