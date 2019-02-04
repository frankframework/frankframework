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
package nl.nn.adapterframework.configuration.classloader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;

public abstract class ClassLoaderTestBase<C extends ClassLoader> {

	private ClassLoader C = null;

	abstract C createClassLoader(ClassLoader parent) throws Exception;

	@Before
	public void init() throws Exception {
		ClassLoader parent = new ClassLoaderMock();
		C = createClassLoader(parent);
	}

	/**
	 * Returns the scheme, defaults to <code>file</code>
	 * @return scheme to test against
	 */
	protected String getScheme() {
		return "file";
	}

	public void resourceExists(String resource) {
		resourceExists(resource, getScheme());
	}
	public void resourceExists(String resource, String scheme) {
		URL url = C.getResource(resource);
		assertNotNull("cannot find resource", url);
		String file = url.toString();
		assertTrue("scheme["+scheme+"] is wrong for file["+file+"]", file.startsWith(scheme + ":"));
		assertTrue("name is wrong", file.endsWith(resource));
	}

	public void resourcesExists(String name) throws IOException {
		resourcesExists(name, getScheme());
	}
	public void resourcesExists(String name, String scheme) throws IOException {
		LinkedList<String> schemes = new LinkedList<String>();
		if(!scheme.equals("scheme"))
			schemes.add(scheme);
		schemes.add("file");
		resourcesExists(name, schemes);
	}

	/**
	 * ORDER MATTERS HERE!
	 * @param name
	 * @param schemes
	 * @throws IOException
	 */
	public void resourcesExists(String name, LinkedList<String> schemes) throws IOException {
		Enumeration<URL> resources = C.getResources(name);
		while(resources.hasMoreElements()) {
			URL url = resources.nextElement();
			assertNotNull("cannot find resource", url);
			String file = url.toString();
			String scheme = schemes.removeFirst();
			assertTrue("scheme["+scheme+"] is wrong for file["+file+"]", file.startsWith(scheme + ":"));
			assertTrue("name is wrong", file.endsWith(name));
		}
	}

	@Test
	public void fileNotFound() {
		assertNull(C.getResource("not-found.txt"));
	}

	/* getResource() */
	@Test
	public void testFile() {
		resourceExists("file");
	}

	@Test
	public void testFileTxt() {
		resourceExists("file.txt");
	}

	@Test
	public void textFileXml() {
		resourceExists("file.xml");
	}

	@Test
	public void textFolderFile() {
		resourceExists("folder/file");
	}

	@Test
	public void textFolderFileTxt() {
		resourceExists("folder/file.txt");
	}

	@Test
	public void textFolderFileXml() {
		resourceExists("folder/file.xml");
	}

	@Test
	public void parentOnlyFile() {
		resourceExists("parent_only.xml", "file");
	}

	@Test
	public void parentOnlyFolder() {
		resourceExists("folder/parent_only.xml", "file");
	}
}
