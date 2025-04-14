/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.larva.actions;

import java.net.URL;

import org.frankframework.configuration.classloaders.DirectoryClassLoader;

/**
 * First searches to the Scenario root directory
 * Then attempts relative paths (to the scenario root directory)
 * Then delegates to it's parent
 */
public class RelativePathDirectoryClassLoader extends DirectoryClassLoader {

	public RelativePathDirectoryClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public URL getResource(String name, boolean useParent) {
		URL url = super.getResource(name, false);
		if(url == null) {
			url = getLocalResource(name);
		}
		if(url == null && useParent) {
			url = getParent().getResource(name);
		}
		return url;
	}
}
