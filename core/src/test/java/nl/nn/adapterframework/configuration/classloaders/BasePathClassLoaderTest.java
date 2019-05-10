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

import org.junit.Test;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;

public class BasePathClassLoaderTest extends ClassLoaderTestBase<BasePathClassLoader> {

	@Override
	BasePathClassLoader createClassLoader(ClassLoader parent) throws Exception {
		return new BasePathClassLoader(parent, ClassLoaderMock.BASEPATH);
	}

	/* test files that are only present on the basepath */
	@Test
	public void classloaderOnlyFile() {
		resourceExists("basepath_only.xml");
	}

	@Test
	public void classloaderOnlyFolder() {
		resourceExists("folder/basepath_only.xml");
	}

	/* test getResources() */
	@Test
	public void testFiles() throws IOException {
		resourcesExists("file");
	}

	@Test
	public void testFilesTxt() throws IOException {
		resourcesExists("file.txt");
	}

	@Test
	public void textFilesXml() throws IOException {
		resourcesExists("file.xml");
	}

	@Test
	public void textFolderFiles() throws IOException {
		resourcesExists("folder/file");
	}

	@Test
	public void textFolderFilesTxt() throws IOException {
		resourcesExists("folder/file.txt");
	}

	@Test
	public void textFolderFilesXml() throws IOException {
		resourcesExists("folder/file.xml");
	}
}
