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
package org.frankframework.extensions.tibco;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serial;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IScopeProvider;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StreamUtil;

/**
 * TibcoProperties are read as a property file, and must be converted (not cast) to the appropriate type.
 */
@Log4j2
public class TibcoEmsProperties extends HashMap<String, Object> {

	@Serial
	private static final long serialVersionUID = 1L;

	public TibcoEmsProperties(IScopeProvider scope, String jndiPropertiesFile) throws IOException {
		this(ClassLoaderUtils.getResourceURL(scope, jndiPropertiesFile, "classpath,file"));
	}

	public TibcoEmsProperties(URL externalURL) throws IOException {
		if(externalURL == null) throw new IOException("file ["+externalURL+"] not found");
		Properties properties = new Properties();
		try (InputStream is = externalURL.openStream(); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
			properties.load(reader);
			log.info("Tibco EMS properties loaded from url [{}]", externalURL::toString);
		}

		for (Object keyObj : properties.keySet()) {
			final String key = (String) keyObj;
			Object parseToValue = parseValue(properties.getProperty(key));
			log.debug("mapped key [{}] to type [{}]", () -> key, () -> parseToValue.getClass().getSimpleName());
			put(key, parseToValue);
		}
	}

	private Object parseValue(String value) {
		if ("true".equals(value) || "false".equals(value)) {
			return Boolean.parseBoolean(value);
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// ignore exception
		}
		return value;
	}
}
