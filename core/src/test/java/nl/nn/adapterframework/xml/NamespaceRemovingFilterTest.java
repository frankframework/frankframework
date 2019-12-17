/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.xml;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;

import nl.nn.adapterframework.util.XmlUtils;

public class NamespaceRemovingFilterTest {

	private boolean TEST_CDATA=true;
	private String CDATA_START=TEST_CDATA?"<![CDATA[":"";
	private String CDATA_END=TEST_CDATA?"]]>":"";

	protected String testString="<root><sub name=\"P &amp; Q €\">abc&amp;€</sub><sub>"+CDATA_START+"<a>a&amp;b€</a>"+CDATA_END+"</sub><!--this is comment--></root>";

	private String messageBasicNoNS="<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub></root>";
	private String messageBasicNS1="<root xmlns=\"urn:test\"><sub name=\"p &amp; Q\">A &amp; B</sub><sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</sub></root>";
	private String messageBasicNS2="<ns:root xmlns:ns=\"urn:test\"><ns:sub name=\"p &amp; Q\">A &amp; B</ns:sub><ns:sub>"+CDATA_START+"<a>a &amp; b</a>"+CDATA_END+"</ns:sub></ns:root>";
	
	

	public void testToWriter(String source, String expected) throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);
		
		NamespaceRemovingFilter filter = new NamespaceRemovingFilter();
		filter.setContentHandler(xmlWriter);
		
		XmlUtils.parseXml(filter, source);

		String actual = new String (target.toString());
		assertEquals(expected, actual);
	}
	
	@Test
	public void testToWriterNoNamespace() throws Exception {
		testToWriter(messageBasicNoNS,messageBasicNoNS);
	}

	@Test
	public void testToWriterNamespacesNoPrefix() throws Exception {
		testToWriter(messageBasicNS1,messageBasicNoNS);
	}

	@Test
	public void testToWriterNamespacePrefixed() throws Exception {
		testToWriter(messageBasicNS2,messageBasicNoNS);
	}
	
}
