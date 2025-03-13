/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StringResolver;
import org.frankframework.util.TransformerPool;

/**
 * This Pipe opens and returns a file from the classpath. The filename is a mandatory parameter to use. You can
 * provide this by using the <code>filename</code> attribute or with a <code>param</code> element to be able to
 * use a sessionKey for instance.
 *
 * <h2>Migrating from deprecated features</h2>
 * The FixedResultPipe was a jack of all trades. You could use it to read a file (only text) and/or use
 * a 'resultString' to find / replace values in. The following migrations are available:
 *
 * <h3>For using a 'resultString'</h3>
 * You can use the {@link EchoPipe} for a static value. This looked like this before:
 *
 * <pre>{@code
 * <pipe name="HelloWorld" className="org.frankframework.pipes.FixedResult" returnString="Hello World">
 *     <forward name="success" path="EXIT"/>
 * </pipe>
 * }</pre>
 * Becomes:
 * <pre>{@code
 * <pipe name="HelloWorld" className="org.frankframework.pipes.EchoPipe" getInputFromFixedValue="Hello World">
 *     <forward name="success" path="EXIT"/>
 * </pipe>
 * }</pre>
 *
 * <h3>For replacing a value</h3>
 * You can use the {@link ReplacerPipe} to replace a value in multiple ways. First, when you need to replace a placeholder with a parameter.
 * This looked like:
 * <pre>{@code
 * <pipe name="make unique message" className="org.frankframework.pipes.FixedResultPipe"
 *     returnString="&lt;msg mid=&quot;MID&quot; action=&quot;ACTION&quot; /&gt;" replaceFixedParams="true">
 *     <param name="MID" sessionKey="mid" />
 *     <param name="ACTION" xpathExpression="request/@action" />
 * </pipe>
 * }</pre>
 *
 * And can now be written like this (note the ?{..} syntax):
 * <pre>{@code
 * <pipe name="make unique message" className="org.frankframework.pipes.ReplacerPipe"
 *     getInputFromFixedValue="&lt;msg mid=&quot;?{MID}&quot; action=&quot;?{ACTION}&quot; /&gt;">
 *     <param name="MID" sessionKey="mid" />
 *     <param name="ACTION" xpathExpression="request/@action" />
 * </pipe>
 * }</pre>
 *
 * When you need to replace a fixed value use the ReplacerPipe with find and replace. This looked like this:
 * <pre>{@code
 * <FixedResultPipe name="InputValidateError"
 *     filename="ManageFileSystem/xml/ErrorMessage.xml"
 *     replaceFrom="%reasonCode" replaceTo="NOT_WELL_FORMED_XML">
 *     <forward name="success" path="EXIT" />
 * </FixedResultPipe>
 * }</pre>
 *
 * And now should be solved like this:
 * <pre>{@code
 * <FixedResultPipe name="InputValidateError"
 *     filename="ManageFileSystem/xml/ErrorMessage.xml">
 *     <forward name="success" path="replaceReasonCode" />
 * </FixedResultPipe>
 * <ReplacerPipe name="replaceReasonCode"
 *     find="%reasonCode"
 *     replace="NOT_WELL_FORMED_XML">
 *     <forward name="success" path="EXIT" />
 * </ReplacerPipe>
 * }</pre>
 * This is also an example of now using two pipes to achieve the same result. Each pipe has its own responsibility.
 *
 * <h2>More complex configurations</h2>
 * In some cases, a combination of the above is needed to achieve what worked before. In some cases, FixedResultPipe
 * was also used to store information in the session. For example, a port of configuration in the JMS listener sender configuration looked like this:
 * <pre>{@code
 * <CompareStringPipe name="compareIdAndCid" >
 *     <param name="operand1" sessionKey="id"/>
 *     <param name="operand2" sessionKey="cid"/>
 *     <forward name="equals" path="IdAndCidSame" />
 *     <forward name="lessthan" path="IdAndCidDifferent" />
 *     <forward name="greaterthan" path="IdAndCidDifferent" />
 * </CompareStringPipe>
 * <FixedResultPipe name="IdAndCidSame" returnString="true" storeResultInSessionKey="IdAndCidSame">
 *     <forward name="success" path="displayKeys" />
 * </FixedResultPipe>
 * <FixedResultPipe name="IdAndCidDifferent" returnString="false" storeResultInSessionKey="IdAndCidSame">
 *     <forward name="success" path="displayKeys" />
 * </FixedResultPipe>
 *
 * <pipe name="displayKeys" className="org.frankframework.pipes.FixedResultPipe"
 *     returnString="branch [BRANCH] Orignal Id [MID] cid [CID] id=cid [SAME]" replaceFixedParams="true">
 *     <param name="BRANCH" sessionKey="originalMessage" xpathExpression="*&#47;@branch" />
 *     <param name="MID" sessionKey="id" />
 *     <param name="CID" sessionKey="cid" />
 *     <param name="SAME" sessionKey="IdAndCidSame" />
 *     <forward name="success" path="EXIT" />
 * </pipe>
 * }</pre>
 *
 * Was rewritten to the following:
 * <pre>{@code
 * <CompareStringPipe name="compareIdAndCid" >
 *     <param name="operand1" sessionKey="id"/>
 *     <param name="operand2" sessionKey="cid"/>
 *     <forward name="equals" path="IdAndCidSame" />
 *     <forward name="lessthan" path="IdAndCidDifferent" />
 *     <forward name="greaterthan" path="IdAndCidDifferent" />
 * </CompareStringPipe>
 *
 * <PutInSessionPipe name="IdAndCidSame" value="true" sessionKey="IdAndCidSame">
 *     <forward name="success" path="putOriginalMessageInSession" />
 * </PutInSessionPipe>
 * <PutInSessionPipe name="IdAndCidDifferent" value="false" sessionKey="IdAndCidSame">
 *     <forward name="success" path="putOriginalMessageInSession" />
 * </PutInSessionPipe>
 *
 * <PutInSessionPipe name="putOriginalMessageInSession" sessionKey="incomingMessage"/>
 *
 * <pipe name="displayKeys" className="org.frankframework.pipes.ReplacerPipe"
 *     getInputFromFixedValue="branch [?{BRANCH}] Original Id [?{MID}] cid [?{CID}] id=cid [?{SAME}]">
 *     <param name="BRANCH" sessionKey="originalMessage" xpathExpression="*&#47;@branch" />
 *     <param name="MID" sessionKey="id" />
 *     <param name="CID" sessionKey="cid" />
 *     <param name="SAME" sessionKey="IdAndCidSame" />
 *     <forward name="success" path="EXIT" />
 * </pipe>
 * }</pre>
 * <p>
 *
 * <h2>The features/documentation of the deprecated features</h2>
 * Produces a fixed result that does not depend on the input message. It may return the contents of a file
 * when <code>filename</code> or <code>filenameSessionKey</code> is specified. Otherwise, the
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
 * Assume that there is a parameter with name <code>xyz</code>. If <code>replaceFixedParams</code> is {@code false}, then
 * each occurrence of <code>?{xyz}</code> is replaced by the parameter's value. Otherwise, the text <code>xyz</code>
 * is substituted. See {@link Parameter} to see how parameter values are determined.</li>
 *
 * <li>If attribute <code>substituteVars</code> is {@code true}, then expressions <code>${...}</code> are substituted using
 * system properties, pipelinesession variables and application properties. Please note that
 * no <code>${...}</code> patterns are left if the initial string came from attribute <code>returnString</code>, because
 * any <code>${...}</code> pattern in attribute <code>returnString</code> is substituted when the configuration is loaded.</li>
 * <li>If attribute <code>styleSheetName</code> is set, then the referenced XSLT stylesheet is applied to the resulting string.</li>
 * </ol>
 * <br/>
 * Many attributes of this pipe reference file names. If a file is referenced by a relative path, the path
 * is relative to the configuration's root directory.
 *
 * @ff.parameters Used for substitution. For a parameter named <code>xyz</code>, the string <code>?{xyz}</code> or
 * <code>xyz</code> (if <code>replaceFixedParams</code> is true) is substituted by the parameter's value.
 */
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
@Forward(name = "filenotfound", description = "the configured file was not found (when this forward isn't specified an exception will be thrown)")
public class FixedResultPipe extends FixedForwardPipe {

	private static final String FILE_NOT_FOUND_FORWARD = "filenotfound";

	private boolean useOldSubstitutionStartDelimiter = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("FixedResultPipe.useOldSubstitutionStartDelimiter", false);
	private @Getter String substitutionStartDelimiter = "?";

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
	 * checks for correct configuration, and translates the filename to
	 * a file, to check existence.
	 * If a filename or filenameSessionKey was specified, the contents of the file is put in the
	 * <code>returnString</code>, so that the <code>returnString</code>
	 * may always be returned.
	 *
	 * @throws ConfigurationException
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		filename = getFilename();

		appConstants = AppConstants.getInstance(getConfigurationClassLoader());

		if (StringUtils.isNotEmpty(getFilename())) {
			URL resource;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, getFilename());
			} catch (Throwable e) {
				throw new ConfigurationException("got exception searching for [" + getFilename() + "]", e);
			}
			if (resource == null) {
				throw new ConfigurationException("cannot find resource [" + getFilename() + "]");
			}
		}

		if(useOldSubstitutionStartDelimiter) {
			if(StringUtils.isBlank(filename)) {
				throw new ConfigurationException("attribute [useOldSubstitutionStartDelimiter] may only be used in combination with attribute [filename]");
			}
			substitutionStartDelimiter = "$";
		}

		if (StringUtils.isEmpty(getFilename()) && StringUtils.isEmpty(getFilenameSessionKey()) && returnString == null) { // allow an empty returnString to be specified
			throw new ConfigurationException("has neither filename nor filenameSessionKey nor returnString specified");
		}
		if (StringUtils.isNotEmpty(getStyleSheetName())) {
			transformerPool = TransformerPool.configureStyleSheetTransformer(this, getStyleSheetName(), 0);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Message resultMessage = null;
		String resultString = getReturnString();

		if (StringUtils.isNotEmpty(getFilenameSessionKey())) {
			filename = session.getString(getFilenameSessionKey());
		} else {
			filename = getFilename();
		}

		message.closeOnCloseOf(session); // avoid connection leaking when the message itself is not consumed.

		if (StringUtils.isNotEmpty(filename)) {
			URL resource;
			try {
				resource = ClassLoaderUtils.getResourceURL(this, filename);
			} catch (Exception e) {
				throw new PipeRunException(this, "got exception searching for [" + filename + "]", e);
			}

			if (resource == null) {
				PipeForward fileNotFoundForward = findForward(FILE_NOT_FOUND_FORWARD);
				if (fileNotFoundForward != null) {
					return new PipeRunResult(fileNotFoundForward, message);
				}
				throw new PipeRunException(this, "cannot find resource [" + filename + "]");
			}

			if (stringBasedOperationNeeded()) {
				try (Message msg = new UrlMessage(resource)) {
					resultString = msg.asString();
				} catch (Exception e) {
					throw new PipeRunException(this, "got exception loading [" + filename + "]", e);
				}
			} else {
				resultMessage = new UrlMessage(resource);
				resultMessage.closeOnCloseOf(session);
			}
		}

		// String based handling, scheduled for removal
		if (stringBasedOperationNeeded()) {
			if (StringUtils.isNotEmpty(getReplaceFrom()) && resultString != null) {
				resultString = resultString.replace(getReplaceFrom(), getReplaceTo());
			}

			if (!getParameterList().isEmpty()) {
				resultString = replaceParameters(resultString, message, session);
			}

			resultString = substituteVars(resultString, session);

			if (transformerPool != null) {
				try {
					resultString = transformerPool.transformToString(resultString);
				} catch (SAXException e) {
					throw new PipeRunException(this, "got error converting string [" + resultString + "] to source", e);
				} catch (IOException | TransformerException e) {
					throw new PipeRunException(this, "got error transforming message [" + resultString + "] with [" + getStyleSheetName() + "]", e);
				}
			}

			resultMessage = new Message(resultString);
		}

		log.debug("returning fixed result [{}]", resultMessage);
		return new PipeRunResult(getSuccessForward(), resultMessage);
	}

	/**
	 * @return whether a string based operation is needed to determine if can return a binary file or not
	 */
	private boolean stringBasedOperationNeeded() {
		return getReturnString() != null
				|| !getParameterList().isEmpty()
				|| isSubstituteVars()
				|| transformerPool != null;
	}

	private String substituteVars(String input, PipeLineSession session) {
		if (isSubstituteVars()) {
			return StringResolver.substVars(input, session, appConstants);
		}

		return input;
	}

	private String replaceParameters(String input, Message message, PipeLineSession session) throws PipeRunException {
		String output = input;

			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for (ParameterValue pv : pvl) {
					output = replaceSingle(output, pv.getName(), pv.asStringValue(""));
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}

		return output;
	}

	private String replaceSingle(String value, String replaceFromValue, String to) {
		final String from = (isReplaceFixedParams()) ? replaceFromValue : substitutionStartDelimiter + "{" + replaceFromValue + "}";

		return value.replace(from, to);
	}

	/**
	 * Should values between ${ and } be resolved. If {@code true}, the search order of replacement values is:
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
	 * See {@code replaceFrom}.
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
	 * If {@code true}, parameter replacement matches <code>name-of-parameter</code>, not <code>?{name-of-parameter}</code>
	 *
	 * @ff.default false
	 */
	@Deprecated(since = "8.2", forRemoval = true)
	@ConfigurationWarning("replaceFixedParams is scheduled for removal. Please use the ReplacerPipe")
	public void setReplaceFixedParams(boolean b) {
		replaceFixedParams = b;
	}

	@Deprecated(since = "8.1", forRemoval = true)
	@ConfigurationWarning("please use ?{key} instead where possible so it's clear when to use properties and when to use session variables")
	public void setUseOldSubstitutionStartDelimiter(boolean old) {
		useOldSubstitutionStartDelimiter = old;
	}
}
