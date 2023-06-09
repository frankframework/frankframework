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

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassLoaderUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Produces a fixed result that does not depend on the input message. It may return the contents of a file
 * when <code>filename</code> or <code>filenameSessionKey</code> is specified. Otherwise the
 * value of attribute <code>returnString</code> is returned.
 * <br/><br/>
 * Using parameters and the attributes of this pipe, it is possible to substitute values. This pipe
 * performs the following steps:
 * <ol>
 * <li>During execution, this pipe first obtains a string based on attributes <code>returnString</code>, <code>filename</code> or <code>filenameSessionKey</code>.</li>
 * <li>The resulting string is transformed according to attributes <code>replaceFrom</code> and <code>replaceTo</code> if set.
 * Please note that the plain value of attribute <code>replaceFrom</code> is matched, no <code>${...}</code> here.</li>
 * <li>The resulting string is substituted based on the parameters of this pipe. This step depends on attribute <code>replaceFixedParams</code>.
 * Assume that there is a parameter with name <code>xyz</code>. If <code>replaceFixedParams</code> is <code>false</code>, then
 * each occurrence of <code>${xyz}</code> is replaced by the parameter's value. Otherwise, the text <code>xyz</code>
 * is substituted. See {@link Parameter} to see how parameter values are determined.</li>
 * <li>If attribute <code>substituteVars</code> is <code>true</code>, then expressions <code>${...}</code> are substituted using
 * system properties, pipelinesession variables and application properties. Please note that
 * no <code>${...}</code> patterns are left if the initial string came from attribute <code>returnString</code>, because
 * any <code>${...}</code> pattern in attribute <code>returnString</code> is substituted when the configuration is loaded.</li>
 * <li>If attribute <code>styleSheetName</code> is set, then the referenced XSLT stylesheet is applied to the resulting string.</li>
 * </ol>
 * <br/>
 * Many attributes of this pipe reference file names. If a file is referenced by a relative path, the path
 * is relative to the configuration's root directory.
 *
 * @ff.parameters Used for substitution. For a parameter named <code>xyz</code>, the string <code>${xyz}</code> or
 * <code>xyz</code> (if <code>replaceFixedParams</code> is true) is substituted by the parameter's value.
 *
 * @ff.forward filenotfound the configured file was not found (when this forward isn't specified an exception will be thrown)
 *
 * @author Johan Verrips
 */
@Category("Basic")
@ElementType(ElementTypes.TRANSLATOR)
public class FixedResultPipe extends FixedForwardPipe {

	private static final String FILE_NOT_FOUND_FORWARD = "filenotfound";

	private AppConstants appConstants;
	private @Getter String filename;
	private @Getter String filenameSessionKey;
	private @Getter String returnString;
	private @Getter boolean substituteVars;
	private @Getter String replaceFrom;
	private @Getter String replaceTo;
	private @Getter String styleSheetName;
	private @Getter boolean replaceFixedParams;

	private TransformerPool transformerPool;

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
		if (StringUtils.isNotEmpty(getFilename())) {
			URL resource = null;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, getFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for ["+getFilename()+"]", e);
			}
			if (resource==null) {
				throw new ConfigurationException("cannot find resource ["+getFilename()+"]");
			}
			try {
				returnString = StreamUtil.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("got exception loading ["+getFilename()+"]", e);
			}
		}
		if (StringUtils.isEmpty(getFilename()) && StringUtils.isEmpty(getFilenameSessionKey()) && returnString==null) { // allow an empty returnString to be specified
			throw new ConfigurationException("has neither filename nor filenameSessionKey nor returnString specified");
		}
		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			transformerPool = TransformerPool.configureStyleSheetTransformer(this, getStyleSheetName(), 0);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result=getReturnString();
		String filename = getFilename();
		if (StringUtils.isNotEmpty(getFilenameSessionKey())) {
			try {
				filename = session.getMessage(getFilenameSessionKey()).asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "unable to get filename from session key ["+getFilenameSessionKey()+"]", e);
			}
		}
		if (StringUtils.isNotEmpty(filename)) {
			URL resource = null;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, filename);
			} catch (Throwable e) {
				throw new PipeRunException(this,"got exception searching for ["+filename+"]", e);
			}
			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				}
				throw new PipeRunException(this,"cannot find resource ["+filename+"]");
			}
			try {
				Message msg = new UrlMessage(resource);
				result = msg.asString();
			} catch (Throwable e) {
				throw new PipeRunException(this,"got exception loading ["+filename+"]", e);
			}
		}
		if (StringUtils.isNotEmpty(getReplaceFrom()) && result != null) {
			result = result.replace(getReplaceFrom(), getReplaceTo());
		}
		if (!getParameterList().isEmpty()) {
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for(ParameterValue pv : pvl) {
					String replaceFrom;
					if (isReplaceFixedParams()) {
						replaceFrom=pv.getName();
					} else {
						replaceFrom="${"+pv.getName()+"}";
					}
					String to = pv.asStringValue("");
					result= result.replace(replaceFrom, to);
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		message.closeOnCloseOf(session, this); // avoid connection leaking when the message itself is not consumed.
		if (isSubstituteVars()) {
			result=StringResolver.substVars(result, session, appConstants);
		}

		if (transformerPool != null) {
			try{
				result = transformerPool.transform(Message.asSource(result));
			} catch (SAXException e) {
				throw new PipeRunException(this, "got error converting string [" + result + "] to source", e);
			} catch (IOException | TransformerException e) {
				throw new PipeRunException(this, "got error transforming message [" + result + "] with [" + getStyleSheetName() + "]", e);
			}
		}

		log.debug("returning fixed result [{}]", result);
		return new PipeRunResult(getSuccessForward(), result);
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

	@Deprecated
	@ConfigurationWarning("attribute 'fileName' is replaced with 'filename'")
	public void setFileName(String fileName) {
		setFilename(fileName);
	}

	/**
	 * Name of the file containing the result message.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileNameSessionKey' is replaced with 'filenameSessionKey'")
	public void setFileNameSessionKey(String fileNameSessionKey) {
		setFilenameSessionKey(fileNameSessionKey);
	}

	/**
	 * Name of the session key containing the file name of the file containing the result message.
	 */
	public void setFilenameSessionKey(String filenameSessionKey) {
		this.filenameSessionKey = filenameSessionKey;
	}

	/**
	 * Returned message.
	 */
	public void setReturnString(String returnString) {
		this.returnString = returnString;
	}

	/**
	 * If set, every occurrence of this attribute's value is replaced by the value of <code>replaceTo</code>.
	 */
	public void setReplaceFrom(String replaceFrom){
		this.replaceFrom=replaceFrom;
	}

	/**
	 * See <code>replaceFrom</code>.
	 */
	public void setReplaceTo(String replaceTo){
		this.replaceTo=replaceTo;
	}

	/**
	 * File name of XSLT stylesheet to apply.
	 */
	public void setStyleSheetName (String styleSheetName){
		this.styleSheetName=styleSheetName;
	}

	/**
	 * When set <code>true</code>, parameter replacement matches <code>name-of-parameter</code>, not <code>${name-of-parameter}</code>
	 *
	 * @ff.default false
	 */
	public void setReplaceFixedParams(boolean b){
		replaceFixedParams=b;
	}

}
