/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
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
 * Produces a fixed result that does not depend on the input message. It may return the contents of a file
 * when <code>filename</code> or <code>filenameSessionKey</code> is specified. Otherwise the
 * value of attribute <code>returnString</code> is returned.
 *
 * @ff.parameters Parameters are not supported. Please do not use them with this pipe.
 *
 * @ff.forward filenotfound the configured file was not found (when this forward isn't specified an exception will be thrown)
 *
 * @author Johan Verrips
 */
public class FixedResultPipe extends FixedForwardPipe {

	private final static String FILE_NOT_FOUND_FORWARD = "filenotfound";

	AppConstants appConstants;
	private String filename;
	private String filenameSessionKey;
	private String returnString;
	private boolean substituteVars=false;
	private String replaceFrom = null;
	private String replaceTo = null;
	private String styleSheetName = null;
	private boolean lookupAtRuntime=false;
	private boolean replaceFixedParams=false;

	/**
	 * checks for correct configuration, and translates the filename to
	 * a file, to check existence.
	 * If a filename or filenameSessionKey was specified, the contents of the file is put in the
	 * <code>returnString</code>, so that the <code>returnString</code>
	 * may always be returned.
	 * @throws ConfigurationException
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();
		appConstants = AppConstants.getInstance(getConfigurationClassLoader());
		if (StringUtils.isNotEmpty(getFilename()) && !isLookupAtRuntime()) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(this, getFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for ["+getFilename()+"]", e);
			}
			if (resource==null) {
				throw new ConfigurationException("cannot find resource ["+getFilename()+"]");
			}
			try {
				returnString = Misc.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("got exception loading ["+getFilename()+"]", e);
			}
		}
		if ((StringUtils.isEmpty(getFilename())) && (StringUtils.isEmpty(getFilenameSessionKey())) && returnString==null) {  // allow an empty returnString to be specified
			throw new ConfigurationException("has neither filename nor filenameSessionKey nor returnString specified");
		}
		if (StringUtils.isNotEmpty(replaceFrom)) {
			returnString = replace(returnString, replaceFrom, replaceTo );
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result=returnString;
		String filename = null;
		if (StringUtils.isNotEmpty(getFilenameSessionKey())) {
			try {
				filename = session.getMessage(getFilenameSessionKey()).asString();
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "unable to get filename from session key ["+getFilenameSessionKey()+"]", e);
			}
		}
		if (filename == null && StringUtils.isNotEmpty(getFilename()) && isLookupAtRuntime()) {
			filename = getFilename();
		}
		if (StringUtils.isNotEmpty(filename)) {
			URL resource = null;
			try {
				resource = ClassUtils.getResourceURL(this, filename);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception searching for ["+filename+"]", e);
			}
			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				} else {
					throw new PipeRunException(this,getLogPrefix(session)+"cannot find resource ["+filename+"]");
				}
			}
			try {
				result = Misc.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new PipeRunException(this,getLogPrefix(session)+"got exception loading ["+filename+"]", e);
			}
		}
		if (getParameterList()!=null) {
			ParameterValueList pvl;
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this,getLogPrefix(session)+"exception extracting parameters",e);
			}
			for(ParameterValue pv : pvl) {
				String replaceFrom;
				if (isReplaceFixedParams()) {
					replaceFrom=pv.getDefinition().getName();
				} else {
					replaceFrom="${"+pv.getDefinition().getName()+"}";
				}
				result=replace(result,replaceFrom,pv.asStringValue(""));
			}
		}

		message.closeOnCloseOf(session, this); // avoid connection leaking when the message itself is not consumed.
		if (getSubstituteVars()){
			result=StringResolver.substVars(returnString, session, appConstants);
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
				} catch (SAXException|TransformerConfigurationException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error creating transformer from file [" + styleSheetName + "]", e);
				} catch (TransformerException e) {
					throw new PipeRunException(this,getLogPrefix(session)+"got error transforming resource [" + xsltSource.toString() + "] from [" + styleSheetName + "]", e);
				}
			}
		}

		log.debug(getLogPrefix(session)+ " returning fixed result [" + result + "]");

		return new PipeRunResult(getSuccessForward(), result);
	}

	public static String replace (String target, String from, String to) {
		// target is the original string
		// from   is the string to be replaced
		// to	 is the string which will used to replace
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

	/**
	 * Should values between ${ and } be resolved. If true, the search order of replacement values is:
	 * system properties (1), pipelinesession variables (2), application properties (3).
	 * 
	 * @ff.default false
	 */
	public void setSubstituteVars(boolean substitute){
		this.substituteVars=substitute;
	}
	public boolean getSubstituteVars(){
		return this.substituteVars;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String fileName) {
		setFilename(fileName);
	}

	/**
	 * Name of the file containing the result message. Path is relative to the configuration's root directory (file is expected on the classpath).
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getFilename() {
		return filename;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileNameSessionKey' is replaced with 'filenameSessionKey'")
	public void setFileNameSessionKey(String fileNameSessionKey) {
		setFilenameSessionKey(fileNameSessionKey);
	}

	/**
	 * Name of the session key containing the file name of the file containing the result message.
	 * Path is relative to the configuration's root directory (file is expected on the classpath).
	 */
	public void setFilenameSessionKey(String filenameSessionKey) {
		this.filenameSessionKey = filenameSessionKey;
	}
	public String getFilenameSessionKey() {
		return filenameSessionKey;
	}

	/**
	 * Returned message.
	 */
	public void setReturnString(String returnString) {
		this.returnString = returnString;
	}
	public String getReturnString() {
		return returnString;
	}

	public String getReplaceFrom() {
		return replaceFrom;
	}
	/**
	 * If set, every occurrence of this attribute's value is replaced by the value of <code>replaceTo</code>.
	 * Not applied if returned message comes from attribute <code>returnString</code>.
	 */
	public void setReplaceFrom (String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	public String getReplaceTo() {
		return replaceTo;
	}

	/**
	 * See <code>replaceFrom</code>.
	 */
	public void setReplaceTo (String replaceTo){
		this.replaceTo=replaceTo;
	}

	public String getStyleSheetName() {
		return styleSheetName;
	}
	/**
	 * File name of XSLT stylesheet to apply to the value obtained from attributes
	 * <code>returnString</code>, <code>filename</code> and <code>filenameSessionKey</code>. 
	 */
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

	/**
	 * When set <code>true</code>, the lookup of the file will be done at runtime instead of at configuration time.
	 * In that case, attributes <code>replaceFrom</code> and <code>replaceTo</code> do not work.
	 * 
	 * @ff.default false
	 */
	public void setLookupAtRuntime(boolean b){
		lookupAtRuntime=b;
	}
	public boolean isLookupAtRuntime(){
		return lookupAtRuntime;
	}

	/**
	 * When set <code>true</code>, any parameter is used for replacements with <code>name-of-parameter</code> and not <code>${name-of-parameter}</code>.
	 *
	 * @ff.default false
	 */
	public void setReplaceFixedParams(boolean b){
		replaceFixedParams=b;
	}
	public boolean isReplaceFixedParams(){
		return replaceFixedParams;
	}

}
