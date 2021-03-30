/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.stream.json;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;

import org.apache.logging.log4j.Logger;
import org.jsfr.json.JsonSaxHandler;

import nl.nn.adapterframework.util.LogUtil;

public class JsonUtils {
	static Logger log = LogUtil.getLogger(JsonUtils.class);

	
	public static void parseJson(String json, JsonSaxHandler handler) {
		parseJson(new StringReader(json),handler);
	}
	public static void parseJson(InputStream inputStream, JsonSaxHandler handler) {
		JsonParserFactory factory = Json.createParserFactory(null);
		try {
			JsonParser parser = factory.createParser(inputStream);
			parseJson(parser, handler);
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
				log.warn("Could not close inputstream", e);
			}
		}
	}
	public static void parseJson(Reader reader, JsonSaxHandler handler) {
		JsonParserFactory factory = Json.createParserFactory(null);
		try {
			JsonParser parser = factory.createParser(reader);
			parseJson(parser, handler);
		} finally {
			try {
				reader.close();
			} catch (Exception e) {
				log.warn("Could not close inputstream", e);
			}
		}
	}
	public static void parseJson(JsonParser parser, JsonSaxHandler handler) {
		try {
			if (!handler.startJSON()) {
				return;
			}
	
			while (parser.hasNext()) {
				Event event = parser.next();
	
				switch (event) {
	
					case START_ARRAY:
						if (!handler.startArray()) return;
						break;
					case END_ARRAY:
						if (!handler.endArray()) return;
						break;
	
					case START_OBJECT:
						if (!handler.startObject()) return;
						break;
					case END_OBJECT:
						if (!handler.endObject()) return;
						break;
	
					case KEY_NAME:
						if (!handler.startObjectEntry(parser.getString())) return;
						break;
	
					case VALUE_STRING:
						if (!handler.primitive(new ParserPrimitiveHolder(parser.getString()))) return;
						break;
					case VALUE_NUMBER:
						if (!handler.primitive(new ParserPrimitiveHolder(parser.isIntegralNumber()?parser.getLong():parser.getBigDecimal()))) return;
						break;
					case VALUE_TRUE:
						if (!handler.primitive(new ParserPrimitiveHolder(Boolean.TRUE))) return;
						break;
					case VALUE_FALSE:
						if (!handler.primitive(new ParserPrimitiveHolder(Boolean.FALSE))) return;
						break;
					case VALUE_NULL:
						if (!handler.primitive(new ParserPrimitiveHolder(null))) return;
						break;
					default:
						throw new IllegalStateException("Unknown parser event ["+event+"]");
				}
			}
		} finally {
			handler.endJSON();
		}
	}
	
}
