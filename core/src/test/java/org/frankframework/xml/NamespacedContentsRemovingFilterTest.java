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
package org.frankframework.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.frankframework.util.XmlUtils;

public class NamespacedContentsRemovingFilterTest {

	private final boolean TEST_CDATA = true;
	private final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private final String CDATA_END = TEST_CDATA ? "]]>" : "";

	private final String messageBasicNoNS = "<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub>" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub nil=\"true\"/></root>";
	private final String messageBasicNS1 = "<root xmlns=\"urn:test\"><sub name=\"p &amp; Q\">A &amp; B</sub><sub>" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</sub><sub xmlns=\"http://www.w3.org/2001/XMLSchema-instance\" nil=\"true\"/></root>";
	private final String messageBasicNS2 = "<ns:root xmlns:ns=\"urn:test\"><ns:sub name=\"p &amp; Q\">A &amp; B</ns:sub><ns:sub>" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "</ns:sub><sub xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/></ns:root>";

	private final String messageMixed = "<root xmlns:ns=\"urn:test\"><sub noname=\"x\" ns:name=\"p &amp; Q\">A &amp; B</sub><ns:sub>" + CDATA_START + "<a>a &amp; b</a>" + CDATA_END + "<other/></ns:sub><sub xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/></root>";
	private final String messageMixedFiltered = "<root><sub noname=\"x\">A &amp; B</sub><sub/></root>";

	public void testToWriter(String source, String expected) throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);

		NamespacedContentsRemovingFilter filter = new NamespacedContentsRemovingFilter(xmlWriter);

		XmlUtils.parseXml(source, filter);

		String actual = new String (target.toString());
		assertEquals(expected, actual);
	}

	@Test
	public void testToWriterNoNamespace() throws Exception {
		testToWriter(messageBasicNoNS,messageBasicNoNS);
	}

	@Test
	public void testToWriterNamespacesNoPrefix() throws Exception {
		testToWriter(messageBasicNS1,"");
	}

	@Test
	public void testToWriterNamespacePrefixed() throws Exception {
		testToWriter(messageBasicNS2,"");
	}

	@Test
	public void testToWriterMixed() throws Exception {
		testToWriter(messageMixed,messageMixedFiltered);
	}
}
