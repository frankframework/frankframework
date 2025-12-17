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
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MimeType;

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.header.Header;
import com.datasonnet.jsonnet.EvalScope;
import com.datasonnet.jsonnet.Expr;
import com.datasonnet.jsonnet.FileScope;
import com.datasonnet.jsonnet.Path;
import com.datasonnet.jsonnet.Val;
import com.datasonnet.jsonnet.Val.Func;
import com.datasonnet.jsonnet.Val.Obj;
import com.datasonnet.jsonnet.ValScope;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.spi.Library;

import lombok.extern.log4j.Log4j2;
import scala.Option;
import ujson.Value;

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

	private DataSonnetUtil() {
		// Private constructor to prevent instance creations
	}

	/**
	 * DataSonnet library that allows you to call another process from within a DataSonnet translation.
	 * The 'namespace' field is the 'object' in the translation file. For now this has been fixed to 'sender'.
	 *
	 * nb. the extended class 'library' is made in Scala.
	 */
	public static class DataSonnetToSenderConnector extends Library {
		private final List<ISender> senders;
		private final PipeLineSession session;

		public DataSonnetToSenderConnector(@Nonnull List<ISender> senders, @Nonnull PipeLineSession session) {
			this.senders = senders;

			if (senders.stream().anyMatch(s -> StringUtils.isBlank(s.getName()))) {
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
				answer.put(sender.getName(), makeSimpleFunc(List.of("input"), args -> sendMessage(args, sender)));
			}

			return answer;
		}

		private Val sendMessage(List<Val> inputArgs, ISender sender) {
			String arg = inputArgs.stream()
					.map(this::convertToString)
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("no value provided"));

			try {
				Message result = sender.sendMessageOrThrow(Message.asMessage(arg), session);
				MimeType mimeType = MessageUtils.computeMimeType(result);
				if (mimeType != null && mimeType.isCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON)) {
					// TODO: parse JSON to Val instance
					log.warn("Cannot yet translate JSON result to DataSonnet compatible formats, returning as String");
				}
				return new Val.Str(result.asString());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		private String convertToString(Val value) {
			if (value == Val.bool(true)) {
				return "true";
			} else if (value == Val.bool(false)) {
				return "false";
			} else if (value instanceof Val.Num number) {
				return String.valueOf((long) number.value());
			} else if (value instanceof Val.Str stringValue) {
				return stringValue.value();
			} else {
				String someResult = com.datasonnet.jsonnet.Materializer.stringify(value, new DummyEvaluator());
				log.debug("Function parameter type [{}] converted to JSON [{}]", value.getClass().getName(), someResult);
				return someResult;
			}
		}
	}

	private static class DummyEvaluator implements EvalScope {

		@Override
		public void com$datasonnet$jsonnet$EvalScope$_setter_$emptyMaterializeFileScope_$eq(FileScope x$1) {

		}

		@Override
		public void com$datasonnet$jsonnet$EvalScope$_setter_$preserveOrder_$eq(boolean x$1) {

		}

		@Override
		public void com$datasonnet$jsonnet$EvalScope$_setter_$defaultValue_$eq(Value x$1) {

		}

		@Override
		public Val visitExpr(Expr expr, ValScope scope, FileScope fileScope) {
			return null;
		}

		@Override
		public Value materialize(Val v) {
			return null;
		}

		@Override
		public FileScope emptyMaterializeFileScope() {
			return null;
		}

		@Override
		public boolean preserveOrder() {
			return false;
		}

		@Override
		public Value defaultValue() {
			return null;
		}

		@Override
		public scala.collection.immutable.Map<String, Value> extVars() {
			return null;
		}

		@Override
		public Option<String> loadCachedSource(Path p) {
			return null;
		}

		@Override
		public Path wd() {
			return null;
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

		@Nonnull
		private static MediaType convertSpringToDataSonnetMediaType(@Nullable MimeType springMime) {
			if(springMime != null) {
				try {
					return MediaType.parseMediaType(springMime.toString());
				} catch (IllegalArgumentException e) {
					log.debug("unable to parse mimetype", e);
				}
			}
			return MediaTypes.TEXT_PLAIN;
		}

		@Nullable
		private static MimeType getSpringMimeType(Message message, boolean computeMimeType) {
			MimeType mimeType = message.getContext().getMimeType();
			if (mimeType != null) {
				return mimeType;
			}

			if(computeMimeType) {
				MimeType computedType = MessageUtils.computeMimeType(message);
				if(computedType != null && !"octet-stream".equals(computedType.getSubtype())) {
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
