/*
   Copyright 2018, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

/**
 * The point of this classLoader is to always return the same mocked response.
 * That way we can test all individual ClassLoaders the same way.
 * 
 * @author Niels Meijer
 *
 */
public class ClassLoaderMock extends ClassLoader {

	public static final String ROOTDIR = "/dummy/directory/";
	public static final String BASEPATH = "basepath/";

	private Map<String, URL> URLs = null;
	private Map<String, URL> parentURLs = null;

	public ClassLoaderMock() {
		// we don't call super(); we mock all requests!
		URLs = new HashMap<>();
		parentURLs = new HashMap<>();

		populateUrls();
		populateUrls(BASEPATH);

		addFile(URLs, "AppConstants.properties", true);

		addFile(parentURLs, "parent_only.xml");
		addFile(parentURLs, "folder/parent_only.xml");

		addFile(URLs, BASEPATH+"basepath_only.xml");
		addFile(URLs, BASEPATH+"folder/basepath_only.xml");

		//Files to test the WebAppClassLoader
		addFile(URLs, "WebAppClassLoader/ClassLoaderTestFile.xml");
	}

	private void populateUrls() {
		populateUrls("");
	}
	private void populateUrls(String rootDirOrPrefix) {
		addFile(URLs, rootDirOrPrefix+"ClassLoaderTestFile");
		addFile(URLs, rootDirOrPrefix+"ClassLoaderTestFile.txt");
		addFile(URLs, rootDirOrPrefix+"ClassLoaderTestFile.xml");
		addFile(URLs, rootDirOrPrefix+"ClassLoader/ClassLoaderTestFile");
		addFile(URLs, rootDirOrPrefix+"ClassLoader/ClassLoaderTestFile.txt");
		addFile(URLs, rootDirOrPrefix+"ClassLoader/ClassLoaderTestFile.xml");
	}
	private void addFile(Map<String, URL> map, String file) {
		addFile(map, file, false);
	}
	private void addFile(Map<String, URL> map, String file, boolean retrieveFromParent) {
		if(!retrieveFromParent) {
			try {
				map.put(file, new URL("file:"+ROOTDIR+file));
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			String resource = file;
			if(!resource.startsWith("/"))
				resource = "/"+file;

			map.put(file, this.getClass().getResource(resource));
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
		Vector<URL> urls = new Vector<>();

		URL basePathUrl = getResource(name);
		if (basePathUrl != null)
			urls.add(basePathUrl);

		URL parent = parentURLs.get(name);
		if(parent != null)
			urls.add(parent);

		return urls.elements();
	}
}
