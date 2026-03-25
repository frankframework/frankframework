/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.util;

import java.io.InputStream;

import org.apache.commons.lang3.NotImplementedException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

import org.frankframework.management.bus.BusException;

public class JacksonUtils {
	private static final ObjectMapper MAPPER;

	static {
		MAPPER = JsonMapper.builder()
				.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
				.enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
				.build();
	}

	public static String convertToJson(Object payload) {
		try {
			return MAPPER.writeValueAsString(payload);
		} catch (JacksonException e) {
			throw new BusException("unable to convert response to JSON", e);
		}
	}

	public static <T> T convertToDTO(Object payload, Class<T> dto) {
		try {
			return switch (payload) {
				case String string -> MAPPER.readValue(string, dto);
				case byte[] bytes -> MAPPER.readValue(bytes, dto);
				case InputStream stream -> MAPPER.readValue(stream, dto);
				default -> throw new NotImplementedException("unhandled payload type [" + payload.getClass() + "]");
			};
		} catch (JacksonException e) {
			throw new BusException("unable to convert payload", e);
		}
	}
}
