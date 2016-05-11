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
package nl.nn.adapterframework.validation;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * @author Michiel Meeuwissen
 * @since 5.0
 */
public class SchemasProviderImpl implements SchemasProvider {
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final String id;
    private final String xsd;

    public SchemasProviderImpl(String id, String xsd) {
        this.id = id;
        this.xsd = xsd;
    }
    public String getSchemasId() throws ConfigurationException {
        return id;
    }

    public List<Schema> getSchemas() throws ConfigurationException {
        return Collections.<Schema>singletonList(
                new Schema() {
                    public InputStream getInputStream() throws IOException {
                        return ClassUtils.getResourceURL(classLoader, xsd).openStream();
                    }
                    public String getSystemId() {
                        return ClassUtils.getResourceURL(classLoader, xsd).toExternalForm();
                    }
                }
        );
    }

    /**
     * Not clear what this should do.
     */
    public String getSchemasId(IPipeLineSession session) throws PipeRunException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not clear what this should do.
     */
    public List<Schema> getSchemas(IPipeLineSession session) throws PipeRunException {
        throw new UnsupportedOperationException();
    }
}
