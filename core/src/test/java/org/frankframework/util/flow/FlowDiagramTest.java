package org.frankframework.util.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

import org.frankframework.core.Resource;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TransformerPool;
import org.junit.jupiter.api.Test;

public class FlowDiagramTest {

	@Test
	public void testDotGeneratorFlow() throws Exception {
		IFlowGenerator generator = new DotFlowGenerator();
		generator.afterPropertiesSet();

		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineWithoutFirstPipe.xml");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		generator.generateFlow(adapter, out);

		String dot = TestFileUtils.getTestFile("/FlowDiagram/dot.txt");
		assertEquals(dot, new String(out.toByteArray()));
	}

	@Test
	public void testGraphvizGeneratorFlow() throws Exception {
		IFlowGenerator generator = new GraphvizJsFlowGenerator();
		generator.afterPropertiesSet();

		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineWithoutFirstPipe.xml");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		generator.generateFlow(adapter, out);

		String dot = TestFileUtils.getTestFile("/FlowDiagram/pipelineWithoutFirstPipe.svg");
		MatchUtils.assertXmlEquals(dot, new String(out.toByteArray()));
	}

	@Test
	public void testAdapter2DotXslWithoutFirstPipe() throws Exception {
		Resource resource = Resource.getResource("xml/xsl/adapter2dot.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource, 2);
		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineWithoutFirstPipe.xml");
		String dot = TestFileUtils.getTestFile("/FlowDiagram/dot.txt");
		String result = transformerPool.transform(adapter, null);

		assertEquals(dot, result);
	}

	@Test
	public void testAdapter2DotXslExitInMiddle() throws Exception {
		Resource resource = Resource.getResource("xml/xsl/adapter2dot.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource, 2);
		String adapter = TestFileUtils.getTestFile("/FlowDiagram/pipelineExitInTheMiddle.xml");
		String dot = TestFileUtils.getTestFile("/FlowDiagram/dot.txt");
		String result = transformerPool.transform(adapter, null);

		assertEquals(dot, result);
	}

}
