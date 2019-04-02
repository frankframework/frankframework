/*
   Copyright 2016-2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.classloaders;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public abstract class BytesClassLoader extends ClassLoaderBase {
	public static String PROTOCOL = "bytesclassloader";
	protected Logger log = LogUtil.getLogger(this);
	protected Map<String, byte[]> resources = new HashMap<String, byte[]>();

	public BytesClassLoader(ClassLoader classLoader) {
		super(classLoader);
	}

	@Override
	public URL getResource(String name) {
		return getResource(name, true);
	}

	public URL getResource(String name, boolean useParent) {
		name = FilenameUtils.normalize(name, true);
		byte[] bytes = resources.get(name);
		if (bytes != null) {
			URLStreamHandler urlStreamHandler = new BytesURLStreamHandler(bytes);
			try {
				return new URL(null, PROTOCOL + ":" + name, urlStreamHandler);
			} catch (MalformedURLException e) {
				log.error("Could not create url", e);
			}
		}
		if(useParent)
			return super.getResource(name);
		else
			return null;
	}

	public void reload() throws ConfigurationException {
		resources.clear();
	}
}
