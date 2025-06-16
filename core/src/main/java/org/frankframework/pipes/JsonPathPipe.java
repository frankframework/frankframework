/*
   Copyright 2025 WeAreFrank!

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

import java.util.HashMap;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import lombok.Getter;
import net.minidev.json.JSONObject;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

/**
 * Apply a one-liner JSON path expression to the input to extract a value from input data.
 * If the input is in XML format, it will be converted to JSON using the same method as the {@link org.frankframework.align.Xml2Json} pipe.
 * Depending on the result of the expression, this pipe can return a string value or JSON value.
 *
 * <h3>Examples</h3>
 * <p>
 *     <table>
 *         <tr>
 *             <th>JSON Path Expression</th>
 *             <th>Input Message</th>
 *             <th>Output</th>
 *         </tr>
 *         <tr>
 *             <td>{@code $.a}</td>
 *             <td>
 *                <pre><code> {
 *   "a": "Hello World"
 *}</code></pre>
 *             </td>
 *             <td>String with value {@code Hello World}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code $.*.a}</td>
 *             <td>
 *                 <pre><code> {
 *   "k1": {"a": 1},
 *   "k2": {"a": 2}
 *}</code></pre>
 *             </td>
 *             <td>JSON Array with value <pre>{@code [1, 2]}</pre></td>
 *         </tr>
 *         <tr>
 *             <td>{@code $.a}</td>
 *             <td>
 *                 <pre><code> {
 *   "a": {
 *     "Hello": "World"
 *   }
 *}</code></pre>
 *             </td>
 *             <td>JSON Object with value
 *             <pre><code> {
 *   "Hello": "World"
 *}</code></pre>
 *         </tr>
 *     </table>
 *     If the input message does not have a match with the expression, then the Exception Forward path will be taken.
 *
 * </p>
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class JsonPathPipe extends FixedForwardPipe {

	private @Getter String jsonPathExpression;
	private JsonPath jsonPath;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jsonPathExpression == null) {
			throw new ConfigurationException("jsonPathExpression has to be set");
		}
		jsonPath = validateJsonPathExpression(jsonPathExpression);
	}

	@SuppressWarnings("java:S2147") // Cannot combine catches due to the specific exception inheritance tree
	private JsonPath validateJsonPathExpression(String jsonPathExpression) throws ConfigurationException {
		try {
			return JsonPath.compile(jsonPathExpression);
		} catch (com.jayway.jsonpath.InvalidPathException e) {
			throw new ConfigurationException("Invalid JSON Path expression: [" + jsonPathExpression + "]", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		Object result;
		try {
			message.preserve();
			Message jsonMessage = MessageUtils.convertToJsonMessage(message);
			result = jsonPath.read(jsonMessage.asInputStream());
		} catch (Exception e) {
			throw new PipeRunException(this, "Failed to evaluate json path expression [" + jsonPathExpression + "] on input [" + message + "]", e);
		}

		PipeRunResult prr = new PipeRunResult();
		prr.setResult(convertToString(result));
		return prr;
	}

	private String convertToString(Object result) {
		if (result == null) {
			return null;
		}
		if (result instanceof HashMap<?,?> map) {
			@SuppressWarnings("unchecked")
			JSONObject jsonObject = new JSONObject((Map<String, ?>) map);
			return jsonObject.toString();
		}
		return result.toString();
	}

	@Mandatory
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}
}
