/*
   Copyright 2018 Nationale-Nederlanden

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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.classloaders.ReloadAware;

/**
 * The point of this classLoader is to always return the same mocked response.
 * That way we can test all individual ClassLoaders the same way.
 * 
 * @author Niels Meijer
 *
 */
public class ClassLoaderMock extends ClassLoader implements ReloadAware {

	public static final String ROOTDIR = "/dummy/directory/";
	public static final String BASEPATH = "basepath/";

	private Map<String, URL> URLs = null;
	private Map<String, URL> parentURLs = null;

	public ClassLoaderMock() {
		// we don't call super(); we mock all requests!
		URLs = new HashMap<String, URL>();
		parentURLs = new HashMap<String, URL>();

		populateUrls();
		populateUrls(BASEPATH);

		addFile(parentURLs, "parent_only.xml");
		addFile(parentURLs, "folder/parent_only.xml");

		addFile(URLs, BASEPATH+"basepath_only.xml");
		addFile(URLs, BASEPATH+"folder/basepath_only.xml");
	}

	private void populateUrls() {
		populateUrls("");
	}
	private void populateUrls(String rootDirOrPrefix) {
		addFile(URLs, rootDirOrPrefix+"file");
		addFile(URLs, rootDirOrPrefix+"file.txt");
		addFile(URLs, rootDirOrPrefix+"file.xml");
		addFile(URLs, rootDirOrPrefix+"folder/file");
		addFile(URLs, rootDirOrPrefix+"folder/file.txt");
		addFile(URLs, rootDirOrPrefix+"folder/file.xml");
	}
	private void addFile(Map<String, URL> map, String file) {
		try {
			map.put(file, new URL("file:"+ROOTDIR+file));
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public URL getResource(String name) {
		URL url = URLs.get(name);
		if(url == null)
			url = parentURLs.get(name);

		return url;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Vector<URL> urls = new Vector<URL>();

		URL basePathUrl = getResource(name);
		if (basePathUrl != null)
			urls.add(basePathUrl);

		URL parent = parentURLs.get(name);
		if(parent != null)
			urls.add(parent);

		return urls.elements();
	}

	public void reload() throws ConfigurationException {
		//don't do anything
	}
}
