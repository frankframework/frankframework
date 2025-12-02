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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MimeType;

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.header.Header;
import com.datasonnet.jsonnet.Val;
import com.datasonnet.jsonnet.Val.Func;
import com.datasonnet.jsonnet.Val.Obj;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.spi.Library;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.DateParameter;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

@Log4j2
public class DataSonnetUtil {

	public static class DataSonnetToSenderConnector extends Library {
		private List<ISender> senders;
		private PipeLineSession session;

		public DataSonnetToSenderConnector(@Nonnull List<ISender> senders, @Nonnull PipeLineSession session) {
			this.senders = senders;

			if (senders.stream().filter(s -> StringUtils.isBlank(s.getName())).count() > 0) {
				throw new IllegalArgumentException("one or more senders does not have a name");
			}

			this.session = session;
		}

		@Override
		public String namespace() {
			return "sender";
		}

		@Override
		public Map<String, Obj> modules(DataFormatService dataFormats, Header header) {
			return Collections.emptyMap();
		}

		@Override
		public Set<String> libsonnets() {
			return Collections.emptySet();
		}

		@Override
		public Map<String, Func> functions(DataFormatService dataFormats, Header header) {
			Map<String, Val.Func> answer = new HashMap<>();

			for (ISender sender : senders) {
				answer.put(sender.getName(), makeSimpleFunc(List.of("input"), new java.util.function.Function<List<Val>, Val>() {

					@Override
					public Val apply(List<Val> inputArgs) {
						String arg = inputArgs.stream()
								.map(DataSonnetUtil::convertToString)
								.findFirst()
								.orElseThrow(() -> new IllegalArgumentException("no value"));

						try (Message input = Message.asMessage(arg); Message result = sender.sendMessageOrThrow(input, session)) {
							return new Val.Str(result.asString());
						} catch (Exception e) {
							throw new IllegalStateException(e);
						}
					}
				}));
			}

			return answer;
		}
	};

	private static String convertToString(Val value) {
		if (value == Val.bool(true)) {
			return "true";
		} else if (value == Val.bool(false)) {
			return "false";
		} else if (value instanceof Val.Num) {
			Val.Num x = (Val.Num) value;
			double tmp = x.value();
			return String.valueOf((long) tmp);
		} else if (value instanceof Val.Str stringValue) {
			return stringValue.value();
		} else {
			throw new IllegalArgumentException("currently only supports numbers, booleans and string inputs, got: " + value.getClass());
		}
	}

	public static Message transform(Mapper mapper, Message input, ParameterValueList parameterValues, DataSonnetOutputType outputType) throws IOException {
		Map<String, Document<?>> parameters = getParameters(parameterValues);
		Document<String> document = mapper.transform(new DataSonnetUtil.FrankMessageDocument(input, true), parameters, outputType.getMediaType());
		Message output = new Message(document.getContent());
		output.getContext().withMimeType(document.getMediaType().toString());
		return output;
	}

	/**
	 * Loops over all the {@link IParameter Parameters} and converts them to DataSonnet {@link Document Documents}.
	 */
	private static Map<String, Document<?>> getParameters(ParameterValueList parameterValues) {
		return StreamSupport.stream(parameterValues.spliterator(), false)
				.collect(Collectors.toMap(ParameterValue::getName, DataSonnetUtil::toDocument, (prev, next) -> next, HashMap::new));
	}

	/**
	 * In order to maintain the date format, see {@link DateParameter#getFormatString()}. Use asString explicitly.
	 * Messages may contain a context (with a specific MimeType) and should be parsed as-is.
	 * Whatever remains may be parsed as a `raw` value.
	 */
	private static Document<?> toDocument(ParameterValue pv) {
		if(pv.getDefinition() instanceof DateParameter) {
			return new DefaultDocument<>(pv.asStringValue());
		} else if(pv.getDefinition() instanceof Parameter || pv.getValue() instanceof Message) {
			try {
				return new DataSonnetUtil.FrankMessageDocument(pv.asMessage(), true);
			} catch (IOException e) {
				log.warn("unable to read message", e);
				throw new IllegalStateException(e.getMessage());
			}
		}

		return new DefaultDocument<>(pv.getValue(), MediaTypes.APPLICATION_JAVA);
	}

	private static class FrankMessageDocument implements Document<String> {
		private final String message;
		private final MediaType mediaType;

		public FrankMessageDocument(Message message, boolean computeMimeType) throws IOException {
			this(message, convertSpringToDataSonnetMediaType(getSpringMimeType(message, computeMimeType)));
		}

		private FrankMessageDocument(Message message, MediaType mediaType) throws IOException {
			this.mediaType = mediaType;
			this.message = message.asString();
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
