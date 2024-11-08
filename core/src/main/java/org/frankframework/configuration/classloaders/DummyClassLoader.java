/*
   Copyright 2017, 2019 Nationale-Nederlanden

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
package org.frankframework.configuration.classloaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.util.XmlEncodingUtils;

/**
 * Classloader which loads an empty Configuration.
 * <p>
 * Ordinarily only used for local testing. When head IBIS application and
 * Configuration(s) to load are stored in separate projects, the
 * DummyClassLoader can be used in the head IBIS application to prevent a
 * "could not find config" exception.
 * </p>
 *
 * @author Peter Leeuwenburgh
 */

public class DummyClassLoader extends AbstractClassLoader {

	public DummyClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public void configure(IbisContext ibisContext, String configurationName) throws ClassLoaderException {
		super.configure(ibisContext, configurationName);
	}

	@Override
	public URL getLocalResource(String name) {
		if (name.equals(getConfigurationFile())) {
			String config = "<configuration name=\"" + XmlEncodingUtils.encodeChars(getConfigurationName()) + "\" />";
			byte[] bytes = config.getBytes(Charset.defaultCharset());
			URLStreamHandler urlStreamHandler = new BytesURLStreamHandler(bytes);
			try {
				return new URL(null, CLASSPATH_RESOURCE_SCHEME + name, urlStreamHandler);
			} catch (MalformedURLException e) {
				log.error("Could not create url", e);
			}
		}

		return null;
	}
}
