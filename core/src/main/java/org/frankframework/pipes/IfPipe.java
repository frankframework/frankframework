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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import com.jayway.jsonpath.JsonPath;

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
import org.frankframework.json.JsonException;
import org.frankframework.json.JsonUtil;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;

/**
 * <p>Selects a forward based on an expression. The expression type is coupled to the mediaType:</p>
 * <ul>
 *     <li>XML (application/xml) uses Xpath.</li>
 *     <li>JSON (application/json) uses jsonPath.</li>
 * </ul>
 *
 * <p>The XML mediaType is the default type. If you want to use JSON, you need to set this using 'mimeType' in the Message.</p>
 *
 * <h4>Expressions</h4>
 * <p>Expressions are used to select nodes in the given input document. Imagine a collection of books:</p>
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
 *
 * <p>With both expression languages, you'll be able to select one or multiple nodes from this collection.</p>
 *
 * <p>Using this pipe, there are two options. Use it only with an {@code expression} or combine it with an {@code expressionValue}. When using the expression,
 * the pipe evaluates to {@code thenForwardName} when <em>there is a match</em>, even if it is empty. In the given example, this might be one of:</p>
 * <pre>{@code
 *   $.store
 *   $.store.book[1]
 *   $.store.book[?(@.price == 22.99)].author
 *   $.store.book[?(@.category == 'fiction')]
 * }</pre>
 *
 * <h4>expressionValue</h4>
 * <p>When using expression combined with expressionValue, the pipe evaluates to {@code thenForwardName} when <em>the matched value is equal to
 * expressionValue</em>. This needs to be an exact match.</p>
 *
 * <h4>XML/XPATH</h4>
 * <p>Xpath has been around a long time. Information about the syntax can be found everywhere on the internet.
 * The XML implementation wraps the Xpath expression in an XSL. This enables us to use complex expressions which evaluate to true or false instead of
 * being used only as a selector of nodes in the input XML. This is available to be backwards compatible with the {@link XmlIf} pipe.
 * For instance, take the following example input:</p>
 * <pre>{@code
 *   <results>
 *     <result name="test"></result>
 *     <result name="test"></result>
 *   </results>
 * }</pre>
 *
 * <p>Examples with complex expressions might be something like: {@code number(count(/results/result[contains(@name , 'test')])) > 1}, to test if there's more
 * than one node found containing the string 'test'. Please check if a simpler, less error-prone expression like
 * {@code /results/result[contains(@name, 'test')]} can suffice.</p>
 *
 * <h4>Without expression</h4>
 * <p>Without an expression, the default behaviour is to assume the input is a string. The code will try to match the string to an optional regular expression
 * or tries to match the string value to the optional expressionValue.</p>
 *
 * @ff.note Some behaviour has been slightly modified compared to XmlIf!
 *
 * @see <a href="https://github.com/json-path/JsonPath">JsonPath / Jayway implementation including examples</a>
 * @see <a href="https://jsonpath.fly.dev/">JsonPath online evaluator</a>
 * @see <a href="https://www.w3schools.com/xml/xpath_syntax.asp">Xpath syntax</a>
 * @see <a href="https://www.freeformatter.com/xpath-tester.html">Xpath online evaluator</a>
 * @see <a href="https://en.wikipedia.org/wiki/XPath">Xpath information and history</a>
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
	private JsonPath jsonPath;
	private @Getter String xpathExpression = null;
	private @Getter String expressionValue = null;

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
		jsonPath = JsonUtil.compileJsonPath(jsonPathExpression);
	}

	/**
	 * Since XmlIf uses a construction that is essentially an XSLT expression and not an XPath expression, we need to make sure that we can match
	 * the same in this pipe. Meaning, for instance, that xpathExpression '/root' and expressionValue 'test' on input <root>test</root> will actually match.
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

				// If there's a value, there's a match. An empty string is a match against empty input
				return thenForward;
			}
		}

		// if all else fails, try to match the expressionValue on the input message (asString)
		return getForwardForStringInput(message);
	}

	boolean transformationNeeded() {
		return transformerPool != null || StringUtils.isNotBlank(jsonPathExpression);
	}

	/**
	 * Evaluate expression to a single scalar value, cast to a String. Returns NULL if the expression did
	 * not match anything in the input, return an empty string if the expression matched a non-scalar value or empty value.
	 *
	 * @param message {@link Message} to match
	 * @param session {@link PipeLineSession} in which the pipe executes
	 * @return Expression result. NULL if there was no match.
	 * @throws PipeRunException If there was any exception in execution.
	 */
	private @Nullable String getResultString(Message message, PipeLineSession session) throws PipeRunException {
		if (xpathExpression != null && transformerPool != null) {
			try {
				Map<String, Object> parameterValues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parameterValues = parameterList.getValues(message, session, namespaceAware).getValueMap();
				}

				String transform = transformerPool.transformToString(message.asString(), parameterValues, namespaceAware);

				return (StringUtils.isEmpty(transform)) ? null : transform;
			} catch (Exception ioe) {
				throw new PipeRunException(this, "error reading message", ioe);
			}
		} else if (jsonPath != null) {
			try {
				// Try to match the jsonPath expression on the given json string
				return JsonUtil.evaluateJsonPathToSingleValue(jsonPath, message);
			} catch (JsonException je) {
				throw new PipeRunException(this, je.getMessage(), je);
			}
		}
		// No match was found
		return null;
	}

	/**
	 * When using expressions, jsonPath returns a JsonArray, even if there is only one match. Make sure to get a String from it.
	 * If the result is not an array and not a scalar value, then return an empty string. Do not return NULL
	 * when a result was found.
	 */
	private @Nonnull String getJsonPathResult(@Nonnull Object jsonPathResult) {
		if (jsonPathResult instanceof String string) {
			return string;
		}
		if (jsonPathResult instanceof Number number) {
			return number.toString();
		}
		if (jsonPathResult instanceof Boolean bool) {
			return bool.toString();
		}

		if (jsonPathResult instanceof JSONArray jsonArray
				&& !jsonArray.isEmpty()) {
			return getJsonPathResult(jsonArray.get(0));
		}

		// We found something, but it does not have a proper string representation
		// usable for the IF-pipe.
		// Do not return NULL because NULL indicates that nothing is found.
		return "";
	}

	PipeForward getForwardForStringInput(Message message) throws PipeRunException {
		try {
			String inputString = message.asString();

			// If expressionValue is set, try to match it on the input
			if (StringUtils.isNotEmpty(expressionValue)) {
				return inputString.equals(expressionValue) ? thenForward : elseForward;
			}

			// If the input is not empty, use then forward.
			return StringUtils.isNotEmpty(inputString) ? thenForward : elseForward;
		} catch (IOException e) {
			throw new PipeRunException(this, "error reading message", e);
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
	 * Forward returned when output is {@code true}.
	 *
	 * @ff.default then
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use the 'then' forward in your configuration")
	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	/**
	 * Forward returned when output is {@code false}.
	 *
	 * @ff.default else
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use the 'else' forward in your configuration")
	public void setElseForwardName(String elseForwardName) {
		this.elseForwardName = elseForwardName;
	}

	/** A string to compare the result of the xpathExpression (or the input message itself) to. If not specified, a non-empty result leads to the 'then' forward, and an empty result leads to the 'else' forward. */
	public void setExpressionValue(String expressionValue) {
		this.expressionValue = expressionValue;
	}

	/** xpath expression to be applied to the input-message. If not set, no transformation is done when the input message is mediatype XML */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/** jsonPath expression to be applied to the input-message. if not set, no transformation is done when the input message is mediatype JSON */
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}

	/**
	 * If set to {@code 2} or {@code 3}, a Saxon (net.sf.saxon) XSLT processor 2.0 or 3.0 respectively will be used. Otherwise, XSLT processor 1.0 (org.apache.xalan) will be used.
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
	 * Controls the namespace-awareness of the XSLT transformation.
	 *
	 * @ff.default true
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * @param defaultMediaType The default media type to use when the media type of the message could not be determined.
	 * @ff.default DefaultMediaType.XML
	 */
	public void setDefaultMediaType(SupportedMediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}
}
