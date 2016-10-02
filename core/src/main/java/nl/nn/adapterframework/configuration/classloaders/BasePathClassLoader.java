/*
   Copyright 2016 Nationale-Nederlanden

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

import java.net.URL;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class BasePathClassLoader extends ClassLoader implements ReloadAware {
	private String basePath;
	
	public BasePathClassLoader(ClassLoader parent, String basePath) {
		super(parent);
		this.basePath = basePath;
	}

	@Override
	public URL getResource(String name) {
		URL url = getParent().getResource(basePath + name);
		if (url != null) {
			return url;
		} else {
			return getParent().getResource(name);
		}
	}

	public void reload() throws ConfigurationException {
		if (getParent() instanceof ReloadAware) {
			((ReloadAware)getParent()).reload();
		}
	}
}
