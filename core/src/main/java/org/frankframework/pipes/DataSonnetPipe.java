/*
   Copyright 2024-2026 WeAreFrank!

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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

import com.datasonnet.MapperBuilder;
import com.datasonnet.document.Document;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ISender;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Mandatory;
import org.frankframework.json.DataSonnetOutputType;
import org.frankframework.json.DataSonnetUtil;
import org.frankframework.json.DataSonnetUtil.DataSonnetToSenderConnector;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.Misc;

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
 * <p>
 * This pipe can also call senders using their names as functions in the JSonnet file. For example:
 * <pre>{@code
 * <DataSonnetPipe>
 *     <EchoSender name="myFunction" />
 * </DataSonnetPipe>
 * }</pre>
 * <pre>{@code
 * [
 *   {
 *     "number": sender.myFunction(x)
 *   } for x in [ 1, 2, 3, 4 ]
 * ]
 * }</pre>
 * results in {@code [{"number":"1"},{"number":"2"},{"number":"3"},{"number":"4"}]}.
 *
 * </p>
 *
 * @ff.parameters All parameters are added to the {@code .jsonnet} stylesheet. Parameter names must be unique.
 *
 * @see <a href="https://jsonnet.org/">https://jsonnet.org/</a> for live examples.
 * @see <a href="https://datasonnet.github.io/datasonnet-mapper/datasonnet/latest/cookbook.html">DataSonnet cookbook</a>.
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class DataSonnetPipe extends FixedForwardPipe {
	private String styleSheetName;
	private String resolvedStyleSheet;

	private final List<ISender> senderList = new ArrayList<>();

	private DataSonnetOutputType outputFileFormat = DataSonnetOutputType.JSON;

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		for (ISender sender: senderList) {
			sender.configure();
		}

		resolvedStyleSheet = Misc.getStyleSheet(this, styleSheetName);
	}

	@Override
	public void start() {
		senderList.forEach(ISender::start);

		super.start();
	}

	@Override
	public void stop() {
		super.stop();

		senderList.forEach(ISender::stop);
	}

	@NonNull
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = getParameters(message, session);

		MapperBuilder builder = new MapperBuilder(resolvedStyleSheet)
				.withInputNames(getParameterList().getParameterNames());

		if (!senderList.isEmpty()) {
			builder.withLibrary(new DataSonnetToSenderConnector(senderList, session));
		}

		try {
			Message output = DataSonnetUtil.transform(builder.build(), message, pvl, outputFileFormat);
			return new PipeRunResult(getSuccessForward(), output);
		} catch (Exception e) { // Typically an IllegalArgumentException
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
	 * DataSonnet is semi-capable of converting the converted JSON to a different format.
	 *
	 * @ff.default JSON
	 */
	public void setOutputFileFormat(DataSonnetOutputType outputType) {
		this.outputFileFormat = outputType;
	}

	@Deprecated
	@ConfigurationWarning("for documentation purposes we've renamed this field to [outputFileFormat]")
	public void setOutputType(DataSonnetOutputType outputType) {
		setOutputFileFormat(outputType);
	}

	@Deprecated // replaced by setSender, to allow for multiple senders in XSD. Method must be present, as it is used by Digester
	public final void setSender(ISender sender) {
		addSender(sender);
	}

	/** One or more specifications of senders. Can be called from the JSonnet file using {@code sender.'name-here'(input)}. */
	public void addSender(ISender sender) {
		senderList.add(sender);
	}
}
