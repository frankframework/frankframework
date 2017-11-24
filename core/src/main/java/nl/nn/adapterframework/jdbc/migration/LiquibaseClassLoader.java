/*
Copyright 2017 Integration Partners B.V.

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
package nl.nn.adapterframework.jdbc.migration;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;

public class LiquibaseClassLoader extends ClassLoader {
	
	public LiquibaseClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	public URL getResource(String name) {
		if(getParent() instanceof BasePathClassLoader)
			return ((BasePathClassLoader) getParent()).getResource(name, false);
		return super.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Vector<URL> urls = new Vector<URL>();
		if(getResource(name) != null)
			urls.add(getResource(name));
		return urls.elements();
	}
}
