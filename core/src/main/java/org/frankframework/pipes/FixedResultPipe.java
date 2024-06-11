/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringResolver;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;
import org.xml.sax.SAXException;

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
 * Please note that the plain value of attribute <code>replaceFrom</code> is matched, no <code>?{...}</code> here.</li>
 *
 * <li>The resulting string is substituted based on the parameters of this pipe. This step depends on attribute <code>replaceFixedParams</code>.
 * Assume that there is a parameter with name <code>xyz</code>. If <code>replaceFixedParams</code> is <code>false</code>, then
 * each occurrence of <code>?{xyz}</code> is replaced by the parameter's value. Otherwise, the text <code>xyz</code>
 * is substituted. See {@link Parameter} to see how parameter values are determined.</li>
 *
 * <li>If attribute <code>substituteVars</code> is <code>true</code>, then expressions <code>${...}</code> are substituted using
 * system properties, pipelinesession variables and application properties. Please note that
 * no <code>${...}</code> patterns are left if the initial string came from attribute <code>returnString</code>, because
 * any <code>${...}</code> pattern in attribute <code>returnString</code> is substituted when the configuration is loaded.</li>
 * <li>If attribute <code>styleSheetName</code> is set, then the referenced XSLT stylesheet is applied to the resulting string.</li>
 * </ol>
 * <br/>
 * Many attributes of this pipe reference file names. If a file is referenced by a relative path, the path
 * is relative to the configuration's root directory.
 * <p>
 * New behaviour (after removal of deprecated attributes):
 * This Pipe opens and returns a file from the classpath. The filename is a mandatory parameter to use. You can
 * provide this by using the <code>filename</code> attribute or with a <code>param</code> element to be able to
 * use a sessionKey for instance.
 *
 * @author Johan Verrips
 * @ff.parameters The <code>filename</code> parameter is used to specify the file to open from the classpath. This can be an
 * 		absolute or relative path. If a file is referenced by a relative path, the path is relative to the configuration's root directory.
 * @ff.forward filenotfound the configured file was not found (when this forward isn't specified an exception will be thrown)
 */
@Category("Basic")
@ElementType(ElementTypes.TRANSLATOR)
public class FixedResultPipe extends FixedForwardPipe {

	private static final String FILE_NOT_FOUND_FORWARD = "filenotfound";

	private static final String PARAMETER_FILENAME = "filename";

	private static final String SUBSTITUTION_START_DELIMITER = "?";

	private AppConstants appConstants;

	private TransformerPool transformerPool;

	private @Getter String filename;
	private @Getter String filenameSessionKey;
	private @Getter String returnString;
	private @Getter boolean substituteVars;
	private @Getter String replaceFrom;
	private @Getter String replaceTo;
	private @Getter String styleSheetName;
	private @Getter boolean replaceFixedParams;

	/**
	 * checks for correct configuration, and checks whether the given filename actually exists
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		filename = getFilename();

		// Change to this after removal of deprecated attributes:
		// filename = determineFilename();

		appConstants = AppConstants.getInstance(getConfigurationClassLoader());

		if (StringUtils.isNotEmpty(getFilename())) {
			URL resource = null;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, getFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for [" + getFilename() + "]", e);
			}
			if (resource == null) {
				throw new ConfigurationException("cannot find resource [" + getFilename() + "]");
			}

			// For removal
			try {
				returnString = StreamUtil.resourceToString(resource, Misc.LINE_SEPARATOR);
			} catch (Throwable e) {
				throw new ConfigurationException("got exception loading [" + getFilename() + "]", e);
			}
			// End for removal
		}

		// For removal
		if (StringUtils.isEmpty(getFilename()) && StringUtils.isEmpty(getFilenameSessionKey()) && returnString == null) { // allow an empty returnString to be specified
			throw new ConfigurationException("has neither filename nor filenameSessionKey nor returnString specified");
		}
		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			transformerPool = TransformerPool.configureStyleSheetTransformer(this, getStyleSheetName(), 0);
		}
		// End for removal
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		// Remove
		String result = getReturnString();

		// Remove
		if (StringUtils.isNotEmpty(getFilenameSessionKey())) {
			filename = session.getString(getFilenameSessionKey());
		} else {
			filename = getFilename();
		}

		message.closeOnCloseOf(session, this); // avoid connection leaking when the message itself is not consumed.

		if (StringUtils.isNotEmpty(filename)) {
			URL resource;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, filename);
			} catch (Throwable e) {
				throw new PipeRunException(this, "got exception searching for [" + filename + "]", e);
			}

			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				}
				throw new PipeRunException(this, "cannot find resource [" + filename + "]");
			}

			// To be removed
			try (Message msg = new UrlMessage(resource)) {
				result = msg.asString();
			} catch (Throwable e) {
				throw new PipeRunException(this, "got exception loading [" + filename + "]", e);
			}

			// Replace with:
//			Message result = new UrlMessage(resource);
//
//			log.debug("returning fixed result filename [{}]", filename);
//			if (!Message.isNull(result)) {
//				return new PipeRunResult(getSuccessForward(), result);
//			}
		}

		// String based handling, scheduled for removal
		if (StringUtils.isNotEmpty(getReplaceFrom()) && result != null) {
			result = result.replace(getReplaceFrom(), getReplaceTo());
		}

		result = replaceParameters(result, message, session);
		result = substituteVars(result, session);

		if (transformerPool != null) {
			try {
				result = transformerPool.transform(XmlUtils.stringToSourceForSingleUse(result));
			} catch (SAXException e) {
				throw new PipeRunException(this, "got error converting string [" + result + "] to source", e);
			} catch (IOException | TransformerException e) {
				throw new PipeRunException(this, "got error transforming message [" + result + "] with [" + getStyleSheetName() + "]", e);
			}
		}

		log.debug("returning fixed result [{}]", result);
		return new PipeRunResult(getSuccessForward(), result);
		// End for removal. Replace with:
		// return new PipeRunResult(getSuccessForward(), message);
	}

	private String substituteVars(String input, PipeLineSession session) {
		if (isSubstituteVars()) {
			return StringResolver.substVars(input, session, appConstants);
		}

		return input;
	}

	private String replaceParameters(String input, Message message, PipeLineSession session) throws PipeRunException {
		String output = input;

		if (!getParameterList().isEmpty()) {
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for (ParameterValue pv : pvl) {
					output = replaceSingle(output, pv.getName(), pv.asStringValue(""));
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		return output;
	}

	private String replaceSingle(String value, String replaceFromValue, String replaceTo) {
		final String replaceFrom = (isReplaceFixedParams()) ? replaceFromValue : SUBSTITUTION_START_DELIMITER + "{" + replaceFromValue + "}";

		return value.replace(replaceFrom, replaceTo);
	}

// Enable after removal
//	private String determineFilename() throws ConfigurationException {
//		if (StringUtils.isNotEmpty(getFilename())) {
//			return getFilename();
//		}
//
//		ParameterList parameterList = getParameterList();
//		if (parameterList != null && parameterList.findParameter(PARAMETER_FILENAME) != null) {
//			return parameterList.findParameter(PARAMETER_FILENAME).getValue();
//		}
//
//		throw new ConfigurationException("No filename parameter found");
//	}

	/**
	 * Should values between ${ and } be resolved. If true, the search order of replacement values is:
	 * system properties (1), PipelineSession variables (2), application properties (3).
	 *
	 * @ff.default false
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("substituteVars is scheduled for removal. Please use the ReplacerPipe")
	public void setSubstituteVars(boolean substitute) {
		this.substituteVars = substitute;
	}

	/**
	 * Name of the file containing the result message.
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Name of the session key containing the file name of the file containing the result message.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("fileNameSessionKey is scheduled for removal. Please use a <param> if you need a session value")
	public void setFilenameSessionKey(String filenameSessionKey) {
		this.filenameSessionKey = filenameSessionKey;
	}

	/**
	 * Returned message.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("returnString is scheduled for removal. Please use the ReplacerPipe or EchoPipe if you need to control the output string")
	public void setReturnString(String returnString) {
		this.returnString = returnString;
	}

	/**
	 * If set, every occurrence of this attribute's value is replaced by the value of <code>replaceTo</code>.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("replaceFrom is scheduled for removal. Please use the ReplacerPipe")
	public void setReplaceFrom(String replaceFrom) {
		this.replaceFrom = replaceFrom;
	}

	/**
	 * See <code>replaceFrom</code>.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("replaceTo is scheduled for removal. Please use the ReplacerPipe")
	public void setReplaceTo(String replaceTo) {
		this.replaceTo = replaceTo;
	}

	/**
	 * File name of XSLT stylesheet to apply.
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("styleSheetName is scheduled for removal. Please use the XsltPipe")
	public void setStyleSheetName(String styleSheetName) {
		this.styleSheetName = styleSheetName;
	}

	/**
	 * When set <code>true</code>, parameter replacement matches <code>name-of-parameter</code>, not <code>?{name-of-parameter}</code>
	 *
	 * @ff.default false
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("replaceFixedParams is scheduled for removal. Please use the ReplacerPipe")
	public void setReplaceFixedParams(boolean b) {
		replaceFixedParams = b;
	}
}
