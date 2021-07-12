/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.junit.Test;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;
import nl.nn.adapterframework.util.flow.IFlowGenerator;
import nl.nn.adapterframework.util.flow.JavaScriptFlowGenerator;

public class FlowDiagramTest {

	@Test
	public void canInitDefaultWithoutErrors() throws Exception {
		IFlowGenerator generator = new JavaScriptFlowGenerator();
		generator.afterPropertiesSet();

		FlowDiagramManager flow = new FlowDiagramManager() {
			@Override
			protected IFlowGenerator createFlowGenerator() {
				return generator;
			}
		};

		assertNotNull(flow);
		flow.afterPropertiesSet();
	}

	@Test
	public void canInitSVGWithoutErrors() throws Exception {
		IFlowGenerator generator = new JavaScriptFlowGenerator();
		generator.setFileExtension("svG");
		generator.afterPropertiesSet();

		FlowDiagramManager flow = new FlowDiagramManager() {
			@Override
			protected IFlowGenerator createFlowGenerator() {
				return generator;
			}
		};

		assertNotNull(flow);
		flow.afterPropertiesSet();
		assertEquals("svg", generator.getFileExtension());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getUnknownFormat() throws Exception {
		IFlowGenerator generator = new JavaScriptFlowGenerator();
		generator.setFileExtension("application/pdf");
		generator.afterPropertiesSet();

		FlowDiagramManager flow = new FlowDiagramManager() {
			@Override
			protected IFlowGenerator createFlowGenerator() {
				return generator;
			}
		};

		assertNotNull(flow);
		flow.afterPropertiesSet();
	}

	@Test
	public void testAdapter2DotXslWithoutFirstPipe() throws Exception {
		TransformerPool.clearTransformerPools();
		Resource resource = Resource.getResource("xml/xsl/adapter2dot.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource, 2);
		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineWithoutFirstPipe.xml");
		String dot = TestFileUtils.getTestFile("/FlowDiagram/dot.txt");
		String result = transformerPool.transform(adapter, null);

		assertEquals(dot, result);
	}

	@Test
	public void testAdapter2DotXslExitInMiddle() throws Exception {
		TransformerPool.clearTransformerPools();
		Resource resource = Resource.getResource("xml/xsl/adapter2dot.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource, 2);
		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineExitInTheMiddle.xml");
		String dot = TestFileUtils.getTestFile("/FlowDiagram/dot.txt");
		String result = transformerPool.transform(adapter, null);

		assertEquals(dot, result);
	}

}
