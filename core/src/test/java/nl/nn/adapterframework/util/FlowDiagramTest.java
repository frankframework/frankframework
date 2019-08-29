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
package nl.nn.adapterframework.util;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.util.FlowDiagram;

import org.junit.Test;

public class FlowDiagramTest {

	@Test
	public void canInitDefaultWithoutErrors() throws TransformerConfigurationException, IOException {
		FlowDiagram flow = new FlowDiagram();
		assertNotNull(flow);
	}

	@Test
	public void canInitNullWithoutErrors() throws TransformerConfigurationException, IOException {
		FlowDiagram flow = new FlowDiagram(null, null);
		assertNotNull(flow);
	}

	@Test
	public void canInitSVGWithoutErrors() throws TransformerConfigurationException, IOException {
		FlowDiagram flow = new FlowDiagram("svg");
		assertNotNull(flow);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getUnknownFormat() throws TransformerConfigurationException, IOException {
		new FlowDiagram("application/pdf");
	}
}
