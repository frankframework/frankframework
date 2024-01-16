/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package org.frankframework.validation;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StreamUtil;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public class DummySchemasProviderImpl implements SchemasProvider {
	private final IScopeProvider scopeProvider = new TestScopeProvider();
	private final String id;
	private final String xsd;

	public DummySchemasProviderImpl(String id, String xsd) {
		this.id = id;
		this.xsd = xsd;
	}

	@Override
	public String getSchemasId() throws ConfigurationException {
		return id;
	}

	@Override
	public List<Schema> getSchemas() throws ConfigurationException {
		return Collections.<Schema>singletonList(new Schema() {
			@Override
			public Reader getReader() throws IOException {
				URL url = ClassLoaderUtils.getResourceURL(scopeProvider, xsd);
				return StreamUtil.getCharsetDetectingInputStreamReader(url.openStream());
			}

			@Override
			public String getSystemId() {
				return ClassLoaderUtils.getResourceURL(scopeProvider, xsd).toExternalForm();
			}
		});
	}

	@Override
	public String getSchemasId(PipeLineSession session) throws PipeRunException {
		return null;
	}

	@Override
	public List<Schema> getSchemas(PipeLineSession session) throws PipeRunException {
		return null;
	}

}
