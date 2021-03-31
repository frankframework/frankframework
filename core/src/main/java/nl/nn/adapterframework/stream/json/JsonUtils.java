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
import org.xml.sax.SAXException;

import nl.nn.adapterframework.stream.JsonEventHandler;
import nl.nn.adapterframework.util.LogUtil;

public class JsonUtils {
	private static Logger log = LogUtil.getLogger(JsonUtils.class);

	
	public static void parseJson(String json, JsonEventHandler handler) throws SAXException {
		parseJson(new StringReader(json),handler);
	}
	public static void parseJson(InputStream inputStream, JsonEventHandler handler) throws SAXException {
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
	public static void parseJson(Reader reader, JsonEventHandler handler) throws SAXException {
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
	public static void parseJson(JsonParser parser, JsonEventHandler handler) throws SAXException {
		try {
			handler.startDocument();	
			while (parser.hasNext()) {
				Event event = parser.next();
	
				switch (event) {
	
					case START_ARRAY:
						handler.startArray();
						break;
					case END_ARRAY:
						handler.endArray();
						break;
	
					case START_OBJECT:
						handler.startObject();
						break;
					case END_OBJECT:
						handler.endObject();
						break;
	
					case KEY_NAME:
						handler.startObjectEntry(parser.getString());
						break;
	
					case VALUE_STRING:
						handler.primitive(parser.getString());
						break;
					case VALUE_NUMBER:
						if (parser.isIntegralNumber()) {
							handler.primitive(parser.getLong());
						} else {
							handler.primitive(parser.getBigDecimal());
						}
						break;
					case VALUE_TRUE:
						handler.primitive(true);
						break;
					case VALUE_FALSE:
						handler.primitive(false);
						break;
					case VALUE_NULL:
						handler.primitive(null);
						break;
					default:
						throw new IllegalStateException("Unknown parser event ["+event+"]");
				}
			}
		} finally {
			handler.endDocument();
		}
	}
	
}
