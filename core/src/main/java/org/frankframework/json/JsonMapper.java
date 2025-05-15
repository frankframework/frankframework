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
package org.frankframework.json;

import java.io.IOException;
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

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.parameters.DateParameter;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

@Log4j2
public class JsonMapper {

	private Mapper mapper;
	private JsonMapper.DataSonnetOutputType outputType;
	private boolean computeMimeType;

	public enum DataSonnetOutputType {
		JSON(MediaTypes.APPLICATION_JSON),
		CSV(MediaTypes.APPLICATION_CSV),
		XML(MediaTypes.APPLICATION_XML),
		YAML(MediaTypes.APPLICATION_YAML);

		final @Getter MediaType mediaType;
		DataSonnetOutputType(MediaType mediaType) {
			this.mediaType = mediaType;
		}
	}

	public JsonMapper(String dataSonnet, DataSonnetOutputType outputType, boolean computeMimeType, List<String> parameterNames) {
		this.outputType = outputType;
		this.computeMimeType = computeMimeType;
		mapper = new MapperBuilder(dataSonnet)
				.withInputNames(parameterNames)
				.build();
	}

	public Message transform(Message input, ParameterValueList parameterValues) throws IOException {
		Map<String, Document<?>> parameters = getParameters(parameterValues);
		Document<String> document = mapper.transform(new JsonMapper.FrankMessageDocument(input, computeMimeType), parameters, outputType.getMediaType());
		Message output = new Message(document.getContent());
		output.getContext().withMimeType(document.getMediaType().toString());
		return output;
	}

	/**
	 * Loops over all the {@link IParameter Parameters} and converts them to DataSonnet {@link Document Documents}.
	 */
	private Map<String, Document<?>> getParameters(ParameterValueList parameterValues) {
		return StreamSupport.stream(parameterValues.spliterator(), false)
				.collect(Collectors.toMap(ParameterValue::getName, this::toDocument, (prev, next) -> next, HashMap::new));
	}

	/**
	 * In order to maintain the date format, see {@link DateParameter#getFormatString()}. Use asString explicitly.
	 * Messages may contain a context (with a specific MimeType) and should be parsed as-is.
	 * Whatever remains may be parsed as a `raw` value.
	 */
	private Document<?> toDocument(ParameterValue pv) {
		if(pv.getDefinition() instanceof DateParameter) {
			return new DefaultDocument<>(pv.asStringValue());
		} else if(pv.getDefinition() instanceof Parameter || pv.getValue() instanceof Message) {
			return new JsonMapper.FrankMessageDocument(pv.asMessage(), computeMimeType);
		}

		return new DefaultDocument<>(pv.getValue(), MediaTypes.APPLICATION_JAVA);
	}


	public static class FrankMessageDocument implements Document<String> {
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
