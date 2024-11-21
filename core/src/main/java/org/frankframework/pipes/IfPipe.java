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

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.Getter;
import net.minidev.json.JSONArray;

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
import org.frankframework.util.XmlUtils;

/**
 * Selects a forward based on an expression. The expression type is coupled to the mediaType:
 * <ul>
 *     <li>XML (application/xml) uses Xpath</li>
 *     <li>JSON (application/json) uses jsonPath</li>
 * </ul>
 * The XML mediaType is the default type, if you want to use json, you need to set this using 'mimeType' in the Message.
 *
 * <h4>Expressions</h4>
 * Expressions are used to select nodes in the given input document. Imagine a collection of books:
 * <pre>{@code
 * {
 *   "store": {
 *     "book": [
 *       {
 *         "category": "reference",
 *         "author": "Nigel Rees",
 *         "title": "Sayings of the Century",
 *         "price": 8.95
 *       },
 *       {
 *         "category": "fiction",
 *         "author": "Evelyn Waugh",
 *         "title": "Sword of Honour",
 *         "price": 12.99
 *       },
 *       {
 *         "category": "fiction",
 *         "author": "Herman Melville",
 *         "title": "Moby Dick",
 *         "isbn": "0-553-21311-3",
 *         "price": 8.99
 *       },
 *       {
 *         "category": "fiction",
 *         "author": "J. R. R. Tolkien",
 *         "title": "The Lord of the Rings",
 *         "isbn": "0-395-19395-8",
 *         "price": 22.99
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 * <p>
 * With both expression languages you'll be able to select one or multiple nodes from this collection.
 * <br/>
 * Using this pipe there are two options. Use it only with an {@code expression} or combine it with an {@code expressionValue}. When using the expression,
 * the pipe evaluates to {@code thenForwardName} when <em>there is a match</em>, even it is empty. In the given example, this might be one of:
 * <pre>{@code
 *   $.store
 *   $.store.book[1]
 *   $.store.book[?(@.price == 22.99)].author
 *   $.store.book[?(@.category == 'fiction')]
 * }</pre>
 *
 * <h4>expressionValue</h4>
 * When using expression combined with expressionValue, the pipe evaluates to {@code thenForwardName} when the <em>the matched value is equal to
 * expressionValue</em>. This needs to be an exact match.
 * <br/>
 *
 * <h4>XML/XPATH</h4>
 * Xpath has been around a long time, information about the syntax can be found everywhere on the internet.
 * The XML implementation wraps the Xpath expression in an XSL. This enables us to use complex expressions which evaluate to true or false instead of
 * being used only as a selector of nodes in the input XML. This is available to be backwards compatible with the {@link XmlIf} pipe.
 * For instance, take the following example input:
 * <pre>{@code
 *   <results>
 *     <result name="test"></result>
 *     <result name="test"></result>
 *   </results>
 * }</pre>
 * Examples with complex expressions might be something like: {@code number(count(/results/result[contains(@name , 'test')])) > 1}, to test if there's more
 * than one node found containing the string 'test'. Please check if a simpler, less error-prone expression like
 * {@code /results/result[contains(@name, 'test')]} can suffice.
 * <p></p>
 *
 * <h4>Without expression</h4>
 * Without an expression, the default behaviour is to assume the input is a string, the code will try to match the string to an optional regular expression
 * or tries to match the string value to the optional expressionValue.
  * <p></p>
 *
 * <h4>Resources</h4>
 * <ul>
 *     <li><a href="https://github.com/json-path/JsonPath">JsonPath / Jayway implementation including examples</a></li>
 *     <li><a href="https://jsonpath.fly.dev/">JsonPath online evaluator</a></li>
 *     <li><a href="https://www.w3schools.com/xml/xpath_syntax.asp">Xpath syntax</a></li>
 *     <li><a href="https://www.freeformatter.com/xpath-tester.html">Xpath online evaluator</a></li>
 *     <li><a href="https://en.wikipedia.org/wiki/XPath">Xpath information and history</a></li>
 * </ul>
 *
 * @ff.note Some behaviour has been slightly modified compared to XmlIf! See 'Changes compared to the XmlIf pipe'.
 * @see DataSonnetPipe
 */
@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class IfPipe extends AbstractPipe {

	private @Getter String elseForwardName = "else";
	private @Getter String thenForwardName = "then";
	private @Getter PipeForward elseForward;
	private @Getter PipeForward thenForward;

	private @Getter String namespaceDefs = null;
	private @Getter boolean namespaceAware = XmlUtils.isNamespaceAwareByDefault();
	private TransformerPool transformerPool;
	private String jsonPathExpression = null;
	private @Getter String xpathExpression = null;
	private @Getter String expressionValue = null;

	private @Getter String regex = null;
	private @Getter int xsltVersion = XmlUtils.DEFAULT_XSLT_VERSION;

	private SupportedMediaType defaultMediaType = SupportedMediaType.XML;

	public enum SupportedMediaType {
		XML(MediaType.APPLICATION_XML),
		JSON(MediaType.APPLICATION_JSON);

		final MediaType mediaType;

		SupportedMediaType(MediaType mediaType) {
			this.mediaType = mediaType;
		}

		@Override
		public String toString() {
			return this.mediaType.toString();
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		thenForward = assertExistsAndGetForward(thenForwardName);
		elseForward = assertExistsAndGetForward(elseForwardName);

		if (StringUtils.isNotEmpty(xpathExpression)) {
			transformerPool = TransformerPool.configureTransformer0(this, namespaceDefs, determineXpathExpression(), null,
					TransformerPool.OutputType.XML, false, getParameterList(), xsltVersion
			);
		}
	}

	/**
	 * Since XmlIf uses a construction which is essentially an XSLT expressions and not an xpath expression, we need to make sure that we can match
	 * the same in this pipe. Meaning, for instance that xpathExpression '/root' and expressionValue 'test' on input <root>test</root> will actually match.i
	 */
	private String determineXpathExpression() {
		if (StringUtils.isEmpty(expressionValue)) {
			return xpathExpression;
		}

		String xpath = xpathExpression;

		// Only append the value matching if the xpath selector doesn't use any expressions.
		if (xpathExpression.matches("([\\w/]+)")) {
			xpath = xpathExpression.endsWith("/") ? xpath : xpath + "/";
			xpath += "text()";
		}

		return xpath;
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		SupportedMediaType mediaType = getSupportedMediaType(message);

		// Determine the media type and if the correct *pathExpression was provided
		if (jsonPathExpression != null && !SupportedMediaType.JSON.equals(mediaType)
				|| xpathExpression != null && !SupportedMediaType.XML.equals(mediaType)) {
			throw new PipeRunException(this, "Incorrect pathExpression provided for given mediaType " + mediaType);
		}

		PipeForward pipeForward = determineForward(message, session);

		log.debug("resolved forward [{}] to path [{}]", pipeForward.getName(), pipeForward.getPath());

		return new PipeRunResult(pipeForward, message);
	}

	private SupportedMediaType getSupportedMediaType(Message message) {
		MimeType computedType = MessageUtils.computeMimeType(message);

		// check if computedType is one of the supported types, or else use the defaultMediaType
		return Arrays.stream(SupportedMediaType.values())
				.filter(supportedType -> supportedType.mediaType.equals(computedType))
				.findFirst()
				.orElse(defaultMediaType);
	}

	/**
	 * Based on the pipe settings, tries to evaluate the expression and determine the correct forward
	 */
	private PipeForward determineForward(Message message, PipeLineSession session) throws PipeRunException {
		if (transformationNeeded()) {
			String resultAsString = getResultString(message, session);

			// If there's an expressionValue, and the result string is not empty, try to match those
			if (StringUtils.isNoneEmpty(expressionValue, resultAsString)) {
				return resultAsString.equals(expressionValue) ? thenForward : elseForward;
			}

			if (StringUtils.isEmpty(expressionValue)) {
				// if result is null (from selector) or equals 'false' (from expression)
				if (resultAsString == null || StringUtils.equalsIgnoreCase(resultAsString, "false")) {
					return elseForward;
				}

				// If there's a value, there's a match
				return thenForward;
			}
		}

		// if all else fails, this is the legacy behaviour
		return getForwardForStringInput(message);
	}

	private boolean transformationNeeded() {
		return transformerPool != null || StringUtils.isNotBlank(jsonPathExpression);
	}

	private String getResultString(Message message, PipeLineSession session) throws PipeRunException {
		if (xpathExpression != null && transformerPool != null) {
			try {
				Map<String, Object> parameterValues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parameterValues = parameterList.getValues(message, session, namespaceAware).getValueMap();
				}

				String transform = transformerPool.transform(message.asString(), parameterValues, namespaceAware);

				return (StringUtils.isEmpty(transform)) ? null : transform;
			} catch (Exception ioe) {
				throw new PipeRunException(this, "error reading message", ioe);
			}
		} else if (StringUtils.isNotBlank(jsonPathExpression)) {
			// Try to match the jsonPath expression on the given json string
			try {
				Object jsonPathResult = JsonPath.read(message.asInputStream(), jsonPathExpression);

				// if we get to this point, we have a match (and no PathNotFoundException)

				return getJsonPathResult(jsonPathResult);
			} catch (PathNotFoundException e) {
				// No results found for path
			} catch (IOException ioe) {
				throw new PipeRunException(this, "error reading message");
			}
		}

		return null;
	}

	/**
	 * When using expressions, jsonPath returns a JsonArray, even if there's only one match. make sure to get a String from it.
	 */
	private String getJsonPathResult(Object jsonPathResult) {
		if (jsonPathResult instanceof String string) {
			return string;
		}

		if (jsonPathResult instanceof JSONArray jsonArray
				&& !jsonArray.isEmpty()) {
			return jsonArray.get(0).toString();
		}

		return null;
	}

	private PipeForward getForwardForStringInput(Message message) throws PipeRunException {
		// if all else fails, this is the legacy behaviour
		try {
			String inputString = message.asString();

			if (StringUtils.isNotEmpty(regex)) {
				return inputString.matches(regex) ? thenForward : elseForward;
			} else if (StringUtils.isNotEmpty(expressionValue)) {
				return inputString.equals(expressionValue) ? thenForward : elseForward;
			}

			// If the input is not empty, use then forward.
			return StringUtils.isNotEmpty(inputString) ? thenForward : elseForward;
		} catch (IOException e) {
			throw new PipeRunException(this, "error reading message");
		}
	}

	private PipeForward assertExistsAndGetForward(String forwardName) throws ConfigurationException {
		PipeForward forward = findForward(forwardName);

		if (forward != null) {
			return forward;
		}

		throw new ConfigurationException("has no forward with name [" + forwardName + "]");
	}

	/**
	 * forward returned when output is <code>true</code>
	 *
	 * @ff.default then
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use the 'then' forward in your configuration")
	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	/**
	 * forward returned when output is <code>false</code>
	 *
	 * @ff.default else
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use the 'else' forward in your configuration")
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
	 * Regular expression to be applied to the input-message (ignored if either <code>xpathExpression</code> or <code>jsonPathExpression</code> is specified).
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
	public void setDefaultMediaType(SupportedMediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}
}
