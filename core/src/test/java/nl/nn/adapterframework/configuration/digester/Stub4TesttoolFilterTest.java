/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.io.StringWriter;
import java.util.Properties;

import org.junit.Test;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.filters.Stub4TesttoolFilter;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Unit test for the Stub4TesttoolFilter-class
 * This only contains 1 test to verify the filter
 * The XSLT itself is tested in {@link nl.nn.adapterframework.configuration.ConfigurationUtilsTest} 
 * The Filter is also tested implicitly in {@link ConfigurationDigesterTest} 
 */

public class Stub4TesttoolFilterTest {
	private static final String STUB4TESTTOOL_DIRECTORY = "/ConfigurationUtils/stub4testtool";
	private static final String STUB4TESTTOOL_ORIGINAL_FILENAME = "original.xml";
	private static final String STUB4TESTTOOL_EXPECTED_FILENAME = "expected.xml";
	
	@Test
	public void stub4testtoolFullAdapter() throws Exception {
		String directory = STUB4TESTTOOL_DIRECTORY + "/FullAdapter";
		stub4testtoolTest(directory, false);
	}
	
	public void stub4testtoolTest(String baseDirectory, boolean disableValidators) throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);
		
		Properties properties = new Properties();
		properties.setProperty(ConfigurationUtils.STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		properties.setProperty(ConfigurationUtils.STUB4TESTTOOL_VALIDATORS_DISABLED_KEY, Boolean.toString(disableValidators));
		
		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_ORIGINAL_FILENAME);
		
		ContentHandler filter = Stub4TesttoolFilter.getStub4TesttoolContentHandler(xmlWriter, properties);
		
		XmlUtils.parseXml(originalConfiguration, filter);
		
		String actual = new String(target.toString());

		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/" + STUB4TESTTOOL_EXPECTED_FILENAME);
		MatchUtils.assertXmlEquals(expectedConfiguration, actual);
	}
}
