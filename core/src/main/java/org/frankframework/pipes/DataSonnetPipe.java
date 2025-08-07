/*
   Copyright 2024-2025 WeAreFrank!

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

import com.datasonnet.document.Document;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.json.DataSonnetOutputType;
import org.frankframework.json.JsonMapper;
import org.frankframework.json.JsonUtil;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * <p>
 * Using {@code .jsonnet} transformation files, the DataSonnetPipe uses JSonnet at its core to transform files
 * from and to different file formats specified by supported {@link DataSonnetOutputType outputTypes}.
 * </p>
 * <p>
 * The pipe input message will be set to the JSON object called {@code payload}.
 * It is required for the input message to have a correct MimeType; otherwise, the text will be interpreted as a String.
 * </p>
 * <p>
 * Input message (JSON) format:
 * <pre>{@code
 * {
 *   "userId" : "123",
 *   "name" : "DataSonnet"
 * }
 * }</pre>
 *
 * Jsonnet stylesheet:
 * <pre>{@code
 * {
 *   "uid": payload.userId,
 *   "uname": payload.name,
 * }
 * }</pre>
 * Produces the following JSON output:
 * <pre>{@code
 * {
 *   "uid": "123",
 *   "uname": "DataSonnet"
 * }
 * }</pre>
 * </p>
 *
 * @ff.parameters All parameters are added to the {@code .jsonnet} stylesheet. Parameter names must be unique.
 *
 * @see <a href="https://jsonnet.org/">https://jsonnet.org/</a> for live examples.
 * @see <a href="https://datasonnet.github.io/datasonnet-mapper/datasonnet/latest/cookbook.html">DataSonnet cookbook</a>.
 */
@Log4j2
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class DataSonnetPipe extends FixedForwardPipe {
	private String styleSheetName;
	private JsonMapper mapper;
	private DataSonnetOutputType outputType = DataSonnetOutputType.JSON;
	private boolean computeMimeType = false;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		mapper = JsonUtil.buildJsonMapper(this, styleSheetName, outputType, computeMimeType, getParameterList());
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = getParameters(message, session);

		try {
			Message output = mapper.transform(message, pvl);
			return new PipeRunResult(getSuccessForward(), output);
		} catch (Exception e) {
			throw new PipeRunException(this, "error transforming input", e);
		}
	}

	/**
	 * Loops over all the {@link IParameter Parameters} and converts them to DataSonnet {@link Document Documents}.
	 */
	private ParameterValueList getParameters(Message message, PipeLineSession session) throws PipeRunException {
		try {
			ParameterList parameterList = getParameterList();
			return parameterList.getValues(message, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}
	}

	/** Location of the stylesheet to apply to the input message. */
	@Mandatory
	public void setStyleSheetName(String stylesheetName) {
		this.styleSheetName = stylesheetName;
	}

	/**
	 * Output file format. DataSonnet is semi-capable of converting the converted JSON to a different format.
	 *
	 * @ff.default JSON
	 */
	public void setOutputType(DataSonnetOutputType outputType) {
		this.outputType = outputType;
	}

	/**
	 * Computes the mimetype when it is unknown. It requires more computation.
	 */
	public void setComputeMimeType(boolean computeMimeType) {
		this.computeMimeType = computeMimeType;
	}

}
