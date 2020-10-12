/*
   Copyright 2020 WeAreFrank!

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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.XmlUtils;

public class FrankDigesterRulesTest extends Mockito {
	private class DummyDigesterRulesParser extends DigesterRulesHandler {
		private List<DigesterRule> rules = new ArrayList<>();

		@Override
		protected void handle(DigesterRule rule) {
			rules.add(rule);
		}

		public int size() {
			return rules.size();
		}
	}

	@Test
	public void parseDigesterRulesXml() {
		DummyDigesterRulesParser handler = new DummyDigesterRulesParser();
		Resource digesterRules = Resource.getResource("digester-rules.xml");

		try {
			XmlUtils.parseXml(digesterRules.asInputSource(), handler);
		} catch (IOException e) {
			throw new IllegalStateException("unable to open digesterRules file", e);
		} catch (SAXException e) {
			throw new IllegalStateException("unable to parse digesterRules file", e);
		}

		assertTrue("must at least have 33 patterns", handler.size() >= 33);
	}
}
