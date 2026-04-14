/*
  Copyright 2026 WeAreFrank!

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
package org.frankframework.cache;

import static org.frankframework.cache.AbstractCacheAdapter.PARAM_KEY;
import static org.frankframework.cache.AbstractCacheAdapter.PARAM_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool.OutputType;

public class EhCacheTransformTest {


	@Test
	public void transformKeyUsesKeyParameterBeforeDeprecatedSettings() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-key-parameter-" + System.nanoTime());
		cache.setKeyInputSessionKey("legacyKey");
		cache.addParameter(new Parameter(PARAM_KEY, "resolvedKey"));
		cache.configure();
		cache.open();

		try {
			PipeLineSession session = new PipeLineSession();
			session.put("legacyKey", "legacyKeyValue");

			assertEquals("resolvedKey", cache.transformKey("ignoredInput", session));
		} finally {
			cache.close();
		}
	}

	@Test
	public void transformKeyUsesDeprecatedSessionKeyWhenNoKeyParameter() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-key-deprecated-" + System.nanoTime());
		cache.setKeyInputSessionKey("legacyKey");
		cache.configure();
		cache.open();

		try {
			PipeLineSession session = new PipeLineSession();
			session.put("legacyKey", "legacyKeyValue");

			assertEquals("legacyKeyValue", cache.transformKey("ignoredInput", session));
		} finally {
			cache.close();
		}
	}

	@Test
	public void transformKeyReturnsEmptyWhenBlankParameterAndEmptyKeysAreAllowed() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-key-empty-" + System.nanoTime());
		cache.setCacheEmptyKeys(true);
		cache.addParameter(new Parameter(PARAM_KEY, "  "));
		cache.configure();
		cache.open();

		try {
			assertEquals("", cache.transformKey("ignoredInput", new PipeLineSession()));
		} finally {
			cache.close();
		}
	}

	@Test
	public void transformValueUsesValueParameterBeforeDeprecatedSettings() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-value-parameter-" + System.nanoTime());
		cache.setValueInputSessionKey("legacyValue");
		cache.addParameter(new Parameter(PARAM_VALUE, "resolvedValue"));
		cache.configure();
		cache.open();

		try {
			PipeLineSession session = new PipeLineSession();
			session.put("legacyValue", "legacyValueFromSession");

			assertEquals("resolvedValue", cache.transformValue(new Message("ignoredInput"), session));
		} finally {
			cache.close();
		}
	}

	@Test
	public void transformValueReturnsNullWhenBlankParameterAndEmptyValuesAreNotAllowed() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-value-null-" + System.nanoTime());
		cache.addParameter(new Parameter(PARAM_VALUE, "\t"));
		cache.configure();
		cache.open();

		try {
			assertNull(cache.transformValue(new Message("ignoredInput"), new PipeLineSession()));
		} finally {
			cache.close();
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void transformKeyUsesConfiguredKeyXPathWhenNoKeyParameterIsPresent() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-key-xpath-" + System.nanoTime());
		cache.setKeyXPath("/root/key/text()");
		cache.configure();
		cache.open();

		try {
			assertEquals("xpathKey", cache.transformKey("<root><key>xpathKey</key></root>", new PipeLineSession()));
		} finally {
			cache.close();
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void transformValueUsesConfiguredValueXPathWhenNoValueParameterIsPresent() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-value-xpath-" + System.nanoTime());
		cache.setValueXPath("/root/value/text()");
		cache.setValueXPathOutputType(OutputType.TEXT);
		cache.configure();
		cache.open();

		try {
			assertEquals("xpathValue", cache.transformValue(new Message("<root><value>xpathValue</value></root>"), new PipeLineSession()));
		} finally {
			cache.close();
		}
	}

	@Test
	@SuppressWarnings("deprecation")
	public void transformValueReturnsEmptyStringWhenConfiguredValueXPathIsEmptyAndCacheEmptyValuesIsTrue() throws ConfigurationException {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-value-xpath-empty-" + System.nanoTime());
		cache.setValueXPath("/root/value/text()");
		cache.setValueXPathOutputType(OutputType.TEXT);
		cache.setCacheEmptyValues(true);
		cache.configure();
		cache.open();

		try {
			assertEquals("", cache.transformValue(new Message("<root/>"), new PipeLineSession()));
		} finally {
			cache.close();
		}
	}

	@Test
	public void transformedKeyAndValueCanBeUsedForEhCacheStorage() throws Exception {
		EhCache<String> cache = new EhCache<>();
		cache.setName("transform-storage-" + System.nanoTime());
		cache.addParameter(new Parameter(PARAM_KEY, "cacheKey"));
		cache.addParameter(new Parameter(PARAM_VALUE, "cacheValue"));
		cache.configure();
		cache.open();

		try {
			PipeLineSession session = new PipeLineSession();
			String transformedKey = cache.transformKey("ignoredInput", session);
			String transformedValue = cache.transformValue(new Message("ignoredInput"), session);

			cache.put(transformedKey, transformedValue);

			assertEquals("cacheValue", cache.get("cacheKey"));
		} finally {
			cache.close();
		}
	}

}
