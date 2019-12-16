/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * FixedResultSender, same behaviour as {@link nl.nn.adapterframework.pipes.FixedResult FixedResult}, but now as a ISender.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr>
 *   <td><i>any</i></td><td><i>any</i></td>
 * 	 <td>Any parameters defined on the sender will be used for replacements. Each occurrence
 * 		 of <code>${name-of-parameter}</code> in the file {@link #setFileName(String) fileName} 
 *       will be replaced by its corresponding <i>value-of-parameter</i>. <br>
 *       This works only with files, not with values supplied in attribute {@link #setReturnString(String) returnString}</td>
 * </tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class FixedResultSender extends SenderWithParametersBase {

	private String fileName;
	private String returnString;
	private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;

	/**
	 * checks for correct configuration, and translates the fileName to
	 * a file, to check existence. 
	 * If a fileName was specified, the contents of the file is put in the
	 * <code>returnString</code>, so that allways the <code>returnString</code>
	 * may be returned.
	 * @throws ConfigurationException
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	    
		if (StringUtils.isNotEmpty(fileName)) {
			try {
				returnString = Misc.resourceToString(ClassUtils.getResourceURL(getConfigurationClassLoader(), fileName), SystemUtils.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("Pipe [" + getName() + "] got exception loading ["+fileName+"]", e);
			}
		}
		if ((StringUtils.isEmpty(fileName)) && returnString==null) {  // allow an empty returnString to be specified
			throw new ConfigurationException("Pipe [" + getName() + "] has neither fileName nor returnString specified");
		}
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
	}
 
	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		String result=returnString;
		if (prc!=null) {
			ParameterValueList pvl;
			try {
				pvl = prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException("exception extracting parameters",e);
			}
			if (pvl!=null) {
				for (int i=0; i<pvl.size(); i++) {
					ParameterValue pv = pvl.getParameterValue(i);
					result=replace(result,"${"+pv.getDefinition().getName()+"}",pv.asStringValue(""));
				}
			}
		}

		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, prc.getSession());
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
					throw new SenderException("cannot retrieve ["+ styleSheetName + "], resource [" + xsltSource.toString() + "]", e);
				} catch (TransformerConfigurationException te) {
					throw new SenderException("got error creating transformer from file [" + styleSheetName + "]", te);
				} catch (TransformerException te) {
					throw new SenderException("got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", te);
				} catch (SAXException se) {
					throw new SenderException("caught SAXException", se);
				}
			}
		}

		log.debug("returning fixed result [" + result + "]");
		return result;
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


	@Override
	public boolean isSynchronous() {
		return true;
	}



	@IbisDoc({"should values between ${ and } be resolved from the pipelinesession", "false"})
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
	@IbisDoc({"name of the file containing the resultmessage", ""})
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFileName() {
		return fileName;
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

}
