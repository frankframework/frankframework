/*
   Copyright 2013 Nationale-Nederlanden

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
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * Provides an example of a pipe. It may return the contents of a file
 * (in the classpath) when <code>fileName</code> is specified, otherwise the
 * input of <code>returnString</code> is returned.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedResult</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setFileName(String) fileName}</td>        <td>name of the file containing the resultmessage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReturnString(String) returnString}</td><td>returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstituteVars(boolean) substituteVars}</td><td>Should values between ${ and } be resolved from the PipeLineSession (search order: 1) system properties 2) pipeLineSession variables 3) application properties)</td><td>False</td></tr>
 * <tr><td>{@link #setReplaceFrom(String) replaceFrom}</td><td>string to search for in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplaceTo(String) replaceTo}</td><td>string that will replace each of the strings found in the returned message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>stylesheet to apply to the output message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLookupAtRuntime(boolean) lookupAtRuntime}</td><td>when set <code>true</code>, the lookup of the file will be done at runtime instead of at configuration time</td><td>false</td></tr>
 * </table>
 * </p>
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
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version $Id$
 * @author Johan Verrips
 */
public class FixedResult extends FixedForwardPipe {
	public static final String version="$RCSfile: FixedResult.java,v $ $Revision: 1.25 $ $Date: 2012-06-01 10:52:49 $";
	
    private String fileName;
    private String returnString;
    private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;
	private boolean lookupAtRuntime=false;
	
    /**
     * checks for correct configuration, and translates the fileName to
     * a file, to check existence. 
     * If a fileName was specified, the contents of the file is put in the
     * <code>returnString</code>, so that allways the <code>returnString</code>
     * may be returned.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();
	    
		if (StringUtils.isNotEmpty(getFileName()) && !isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(this,getFileName());
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
        if ((StringUtils.isEmpty(fileName)) && returnString==null) {  // allow an empty returnString to be specified
            throw new ConfigurationException(getLogPrefix(null)+"has neither fileName nor returnString specified");
        }
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
    }
    
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String result=returnString;
		if (StringUtils.isNotEmpty(getFileName()) && isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(this,getFileName());
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception searching for ["+getFileName()+"]", e);
			}
			if (resource==null) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot find resource ["+getFileName()+"]");
			}
			try {
				result = Misc.resourceToString(resource, SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception loading ["+getFileName()+"]", e);
			}
		}

		if (getParameterList()!=null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String)input, session);
			ParameterValueList pvl;
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"exception extracting parameters",e);
			}
			for (int i=0; i<pvl.size(); i++) {
				ParameterValue pv = pvl.getParameterValue(i);
				result=replace(result,"${"+pv.getDefinition().getName()+"}",pv.asStringValue(""));
			}
		}

		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, session, AppConstants.getInstance());
		}

		if (StringUtils.isNotEmpty(styleSheetName)) {
			URL xsltSource = ClassUtils.getResourceURL(this, styleSheetName);
			if (xsltSource!=null) {
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(xsltSource);
					xsltResult = XmlUtils.transformXml(transformer, result);
					result = xsltResult;
				} catch (IOException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"cannot retrieve ["+ styleSheetName + "], resource [" + xsltSource.toString() + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error creating transformer from file [" + styleSheetName + "]", te);
				} catch (TransformerException te) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", te);
				} catch (DomBuilderException te) {
					throw new PipeRunException(this,getLogPrefix(session)+"caught DomBuilderException", te);
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

	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}
	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

    /**
     * Sets the name of the filename. The fileName should not be specified
     * as an absolute path, but as a resource in the classpath.
     *
     * @param fileName the name of the file to return the contents from
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
	public String getFileName() {
		return fileName;
	}

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

	public void setLookupAtRuntime(boolean b){
		lookupAtRuntime=b;
	}
	public boolean isLookupAtRuntime(){
		return lookupAtRuntime;
	}
}
