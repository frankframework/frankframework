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
package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.filters.OnlyActiveFilter;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

public class OnlyActiveFilterTest {

	private final String messageBasicTrue = "<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub active=\"true\"/></root>";
	private final String messageBasicNotFalse = "<root active=\"!false\" ignore=\"me\"><sub name=\"name here\">A &amp; B</sub></root>";
	private final String messageBasicFalse = "<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub active=\"false\"><a>a &amp; b</a></sub><sub/></root>";

	public void testToWriter(String source, String expected) throws Exception {
		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);

		OnlyActiveFilter filter = new OnlyActiveFilter(xmlWriter);

		XmlUtils.parseXml(source, filter);

		String actual = new String (target.toString());
		assertEquals(expected, actual);
	}

	@Test
	public void testToWriterActiveTRUE() throws Exception {
		testToWriter(messageBasicTrue, "<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub/></root>");
	}

	@Test
	public void testToWriterActiveNotFalse() throws Exception {
		testToWriter(messageBasicNotFalse, "<root ignore=\"me\"><sub name=\"name here\">A &amp; B</sub></root>");
	}

	@Test
	public void testToWriterActiveFalse() throws Exception {
		testToWriter(messageBasicFalse, "<root><sub name=\"p &amp; Q\">A &amp; B</sub><sub/></root>");
	}

}
