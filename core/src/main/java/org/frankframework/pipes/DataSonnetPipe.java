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
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.util.MimeType;

import com.datasonnet.Mapper;
import com.datasonnet.MapperBuilder;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.Resource;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.parameters.DateParameter;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

/**
 * <p>
 * Using {@code .jsonnet} transformation files, the DataSonnetPipe uses JSonnet at it's core to transform files
 * from and to different file formats specified by supported {@link DataSonnetOutputType outputTypes}.
 * </p>
 * <p>
 * The pipe input message will be set to the JSON object called {@code payload}.
 * It's required for the input message to have a correct MimeType, else the text will be interpreted as a String.
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
 * @ff.parameters All parameters are added to the {@code .jsonnet} stylesheet, parameter names must be unique.
 * 
 * @see <a href="https://jsonnet.org/">https://jsonnet.org/</a> for live examples.
 * @see <a href="https://datasonnet.github.io/datasonnet-mapper/datasonnet/latest/cookbook.html">DataSonnet cookbook</a>.
 */
@Log4j2
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class DataSonnetPipe extends FixedForwardPipe {
	private String styleSheetName;
	private Mapper mapper;
	private DataSonnetOutputType outputType = DataSonnetOutputType.JSON;
	private boolean computeMimeType = false;

	public enum DataSonnetOutputType {
		JSON(MediaTypes.APPLICATION_JSON),
		CSV(MediaTypes.APPLICATION_CSV),
		XML(MediaTypes.APPLICATION_XML),
		YAML(MediaTypes.APPLICATION_YAML);

		final MediaType mediaType;
		DataSonnetOutputType(MediaType mediaType) {
			this.mediaType = mediaType;
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		List<String> paramNames = getParameterList().stream().map(IParameter::getName).toList();
		mapper = new MapperBuilder(getStyleSheet())
				.withInputNames(paramNames)
				.build();
	}

	private String getStyleSheet() throws ConfigurationException {
		Resource styleSheet = Resource.getResource(this, styleSheetName);
		if (styleSheet == null) {
			throw new ConfigurationException("StyleSheet [" + styleSheetName + "] not found");
		}
		try (InputStream is = styleSheet.openStream()) {
			return StreamUtil.streamToString(is);
		} catch (IOException e) {
			throw new ConfigurationException("unable to open/read StyleSheet [" + styleSheetName + "]", e);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Map<String, Document<?>> parameters = getParameters(message, session);

		try {
			Document<String> document = mapper.transform(new FrankMessageDocument(message, computeMimeType), parameters, outputType.mediaType);
			Message output = new Message(document.getContent());
			output.getContext().withMimeType(document.getMediaType().toString());

			return new PipeRunResult(getSuccessForward(), output);
		} catch (Exception e) {
			throw new PipeRunException(this, "error transforming input", e);
		}
	}

	/**
	 * Loops over all the {@link IParameter Parameters} and converts them to DataSonnet {@link Document Documents}.
	 */
	private Map<String, Document<?>> getParameters(Message message, PipeLineSession session) throws PipeRunException {
		try {
			ParameterList parameterList = getParameterList();
			if(parameterList == null) {
				return Collections.emptyMap();
			}
			ParameterValueList pvl = parameterList.getValues(message, session);

			return StreamSupport.stream(pvl.spliterator(), false)
					.collect(Collectors.toMap(ParameterValue::getName, this::toDocument, (prev, next) -> next, HashMap::new));
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}
	}

	/**
	 * In order to maintain the date format, see {@link DateParameter#getFormatString()} use asString explicitly.
	 * Messages may contain a context (with specific MimeType) and should be parsed as-is.
	 * Whatever remains may be parsed as `raw` value.
	 */
	private Document<?> toDocument(ParameterValue pv) {
		if(pv.getDefinition() instanceof DateParameter) {
			return new DefaultDocument<>(pv.asStringValue());
		} else if(pv.getDefinition() instanceof Parameter || pv.getValue() instanceof Message) {
			return new FrankMessageDocument(pv.asMessage(), computeMimeType);
		}

		return new DefaultDocument<>(pv.getValue(), MediaTypes.APPLICATION_JAVA);
	}

	/** Location of stylesheet to apply to the input message */
	public void setStyleSheetName(String stylesheetName) {
		this.styleSheetName = stylesheetName;
	}

	/**
	 * Output file format, DataSonnet is semi-capable of converting the converted JSON to a different format.
	 */
	public void setOutputType(DataSonnetOutputType outputType) {
		this.outputType = outputType;
	}

	/**
	 * Compute mimetype when unknown. Requires more compute
	 */
	public void setComputeMimeType(boolean computeMimeType) {
		this.computeMimeType = computeMimeType;
	}

	private static class FrankMessageDocument implements Document<String> {
		private final String message;
		private final MediaType mediaType;

		public FrankMessageDocument(Message message, boolean computeMimeType) {
			this(message, convertSpringToDataSonnetMediaType(getSpringMimeType(message, computeMimeType)));
		}

		private FrankMessageDocument(Message message, MediaType mediaType) {
			this.mediaType = mediaType;
			try {
				this.message = message.asString();
			} catch (IOException e) {
				throw new IllegalStateException("unable to read message");
			}
		}

		private static MediaType convertSpringToDataSonnetMediaType(MimeType springMime) {
			if(springMime != null) {
				try {
					return MediaType.parseMediaType(springMime.toString());
				} catch (IllegalArgumentException e) {
					log.debug("unable to parse mimetype", e);
				}
			}
			return MediaTypes.TEXT_PLAIN;
		}

		private static MimeType getSpringMimeType(Message message, boolean computeMimeType) {
			MimeType mimeType = message.getContext().getMimeType();
			if (mimeType != null) {
				return mimeType;
			}

			if(computeMimeType) {
				MimeType computedType = MessageUtils.computeMimeType(message);
				if(!"octet-stream".equals(computedType.getSubtype())) {
					return computedType;
				}
			}

			return null;
		}

		@Override
		public String getContent() {
			return message;
		}

		@Override
		public MediaType getMediaType() {
			return mediaType;
		}

		@Override
		public Document<String> withMediaType(MediaType mediaType) {
			return new DefaultDocument<>(this.getContent(), mediaType);
		}
	}
}
