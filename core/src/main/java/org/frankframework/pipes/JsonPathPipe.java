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

import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.JsonPath;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.json.JsonUtil;
import org.frankframework.stream.Message;

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
 *                <pre>{@code {
 *   "a": "Hello World"
 * } }</pre>
 *             </td>
 *             <td>String with value {@code Hello World}</td>
 *         </tr>
 *         <tr>
 *             <td>{@code $.*.a}</td>
 *             <td>
 *                 <pre>{@code {
 *   "k1": {"a": 1},
 *   "k2": {"a": 2}
 * } }</pre>
 *             </td>
 *             <td>JSON Array with value <pre>{@code [1, 2]}</pre></td>
 *         </tr>
 *         <tr>
 *             <td>{@code $.a}</td>
 *             <td>
 *                 <pre>{@code {
 *   "a": {
 *     "Hello": "World"
 *   }
 * } }</pre>
 *             </td>
 *             <td>JSON Object with value
 *             <pre>{@code {
 *   "Hello": "World"
 * } }</pre>
 *         </tr>
 *     </table>
 *     If the input message does not have a match with the expression, then the Exception Forward path will be taken.
 * </p>
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class JsonPathPipe extends FixedForwardPipe {

	private @Getter String jsonPathExpression;
	private JsonPath jsonPath;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(jsonPathExpression)) {
			throw new ConfigurationException("jsonPathExpression has to be set");
		}
		jsonPath = JsonUtil.compileJsonPath(jsonPathExpression);
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result;
		try {
			message.preserve();
			result = JsonUtil.evaluateJsonPath(jsonPath, message);
		} catch (Exception e) {
			throw new PipeRunException(this, "Failed to evaluate json path expression [" + jsonPathExpression + "] on input [" + message + "]", e);
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	@Mandatory
	public void setJsonPathExpression(String jsonPathExpression) {
		this.jsonPathExpression = jsonPathExpression;
	}
}
