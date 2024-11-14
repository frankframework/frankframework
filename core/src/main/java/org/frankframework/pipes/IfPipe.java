/*
   Copyright 2024 WeAreFrank!

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
import java.util.Arrays;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class IfPipe extends AbstractPipe {

	private String elseForwardName = "else";
	private String expressionValue = null;
	private String namespaceDefs = null;
	private String thenForwardName = "then";
	private boolean namespaceAware = XmlUtils.isNamespaceAwareByDefault();
	private DefaultMediaType defaultMediaType = DefaultMediaType.XML;

	public enum DefaultMediaType {
		XML(MediaType.APPLICATION_XML),
		JSON(MediaType.APPLICATION_JSON);

		final MediaType mediaType;

		DefaultMediaType(MediaType mediaType) {
			this.mediaType = mediaType;
		}
	}

	private TransformerPool transformerPool;
	private String jsonPathExpression = null;
	private String regex = null;
	private String xpathExpression = null;
	private int xsltVersion = XmlUtils.DEFAULT_XSLT_VERSION;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(xpathExpression)) {
			try {
				transformerPool = TransformerPool.getInstance(makeStylesheet(xpathExpression, expressionValue), xsltVersion, this);
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("could not create transformer from xpathExpression [" + xpathExpression + "], target expressionValue [" + expressionValue + "]", e);
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		MimeType mimeType = getMimeType(message);

		// Determine the media type and if the correct *pathExpression was provided
		if (jsonPathExpression != null && !MediaType.APPLICATION_JSON.equals(mimeType)
				|| xpathExpression != null && !MediaType.APPLICATION_XML.equals(mimeType)) {
			throw new PipeRunException(this, "Incorrect pathExpression provided for given mediaType + " + mimeType);
		}

		String forward = determineForward(message, session);

		log.debug("determined forward [{}]", forward);

		PipeForward pipeForward = findForward(forward);
		if (pipeForward == null) {
			throw new PipeRunException(this, "cannot find forward or pipe named [" + forward + "]");
		}

		log.debug("resolved forward [{}] to path [{}]", forward, pipeForward.getPath());
		return new PipeRunResult(pipeForward, message);
	}

	private MimeType getMimeType(Message message) {
		MimeType computedType = MessageUtils.computeMimeType(message);

		if (computedType != null) {
			// check if computedType is one of JSON or XML, if not, fall through
			boolean matchesOneOfSupportedTypes = Arrays.stream(DefaultMediaType.values())
					.anyMatch(supportedType -> supportedType.mediaType.equals(computedType));

			if (matchesOneOfSupportedTypes) {
				return computedType;
			}
		}

		// Default to
		return defaultMediaType.mediaType;
	}

	/**
	 * Based on the pipe settings, tries to evaluate the expression and determine the correct forward
	 */
	private String determineForward(Message message, PipeLineSession session) throws PipeRunException {
		String inputString = getInputString(message);

		if (xpathExpression != null && transformerPool != null) {
			try {
				Map<String, Object> parameterValues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parameterValues = parameterList.getValues(message, session, namespaceAware).getValueMap();
				}
				return transformerPool.transform(inputString, parameterValues, namespaceAware);
			} catch (Exception e) {
				throw new PipeRunException(this, "cannot evaluate expression", e);
			}
		} else if (StringUtils.isNotBlank(jsonPathExpression)) {
			// Try to match the jsonPath expression on the given json string
			try {
				String jsonPathResult = JsonPath.read(inputString, jsonPathExpression);

				// if we get to this point, we have a match (and no PathNotFoundException)

				if (StringUtils.isEmpty(expressionValue)) {
					return thenForwardName;
				}

				// If there's an expressionValue set, it needs to match with the jsonPath query result
				return jsonPathResult.equals(expressionValue) ? thenForwardName : elseForwardName;

			} catch (PathNotFoundException e) {
				// No results found for path
				return elseForwardName;
			}
		}

		// if all else fails, this is the legacy behaviour
		return getForward(inputString);
	}

	private String getInputString(Message message) throws PipeRunException {
		if (Message.isEmpty(message)) {
			return "";
		} else {
			try {
				return message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}
	}

	private String getForward(String inputString) {
		if (StringUtils.isNotEmpty(regex)) {
			return inputString.matches(regex) ? thenForwardName : elseForwardName;
		} else if (StringUtils.isNotEmpty(expressionValue)) {
			return inputString.equals(expressionValue) ? thenForwardName : elseForwardName;
		}

		// If the input is empty, use the else forward.
		return StringUtils.isEmpty(inputString) ? elseForwardName : thenForwardName;
	}

	private String makeStylesheet(String xpathExpression, String resultVal) {
		String namespaceClause = XmlUtils.getNamespaceClause(namespaceDefs);
		return XmlUtils.createXPathEvaluatorSource(x -> "<xsl:choose>" +
						"<xsl:when " + namespaceClause + " test=\"" + XmlEncodingUtils.encodeChars(x) + "\">" + thenForwardName + "</xsl:when>" +
						"<xsl:otherwise>" + elseForwardName + "</xsl:otherwise>" +
						"</xsl:choose>",
				xpathExpression + (StringUtils.isEmpty(resultVal) ? "" : "='" + resultVal + "'"),
				TransformerPool.OutputType.TEXT, false, getParameterList(), true, !namespaceAware, xsltVersion
		);
	}

	/**
	 * forward returned when output is <code>true</code>
	 *
	 * @ff.default then
	 */
	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	/**
	 * forward returned when output is <code>false</code>
	 *
	 * @ff.default else
	 */
	public void setElseForwardName(String elseForwardName) {
		this.elseForwardName = elseForwardName;
	}

	/** a string to compare the result of the xpathExpression (or the input-message itself) to. If not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward */
	public void setExpressionValue(String expressionValue) {
		this.expressionValue = expressionValue;
	}

	/** xpath expression to be applied to the input-message. if not set, no transformation is done when the input message is mediatype XML */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/** jsonPath expression to be applied to the input-message. if not set, no transformation is done when the input message is mediatype JSON */
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}

	/**
	 * Regular expression to be applied to the input-message (ignored if <code>xpathExpression</code> is specified).
	 * The input-message <b>fully</b> matching the given regular expression leads to the 'then'-forward
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use RegExPipe instead")
	public void setRegex(String regex) {
		this.regex = regex;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan)
	 *
	 * @ff.default 2
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion = xsltVersion;
	}

	/** namespace definitions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions. */
	public void setNamespaceDefs(String namespaceDefinitions) {
		this.namespaceDefs = namespaceDefinitions;
	}

	/**
	 * controls namespace-awareness of XSLT transformation
	 *
	 * @ff.default true
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * @param defaultMediaType the default media type to use when the media type of the message could not be determined.
	 * @ff.default DefaultMediaType.XML
	 */
	public void setDefaultMediaType(DefaultMediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}
}
