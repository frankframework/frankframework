package org.frankframework.util.flow.graphviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;

import org.frankframework.core.IScopeProvider;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestScopeProvider;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.flow.FlowGenerationException;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodName.class)
public class GraphvizEngineTest {

	private String dot = "digraph { a -> b[label=\"0.2\",weight=\"0.2\"]; }";
	private IScopeProvider scopeProvider = new TestScopeProvider();

	@Test
	public void canInitDefaultWithoutErrors() throws IOException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		engine.close();
	}

	@Test
	public void canInitNullWithoutErrors() throws IOException {
		GraphvizEngine engine = new GraphvizEngine(null);
		assertNotNull(engine);
		engine.close();
	}

	@Test
	public void happyFlowDot2SVG() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String result = engine.execute(dot);
		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowRender2SVG() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String render = "render('" + dot + "'," + Options.create().toJson(false) + ");";
		String result = engine.execute(render);
		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowDot2SVG_STANDALONE() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String result = engine.execute(dot, Options.create().format(Format.SVG_STANDALONE));
		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.svg_standalone");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowRender2SVG_STANDALONE() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create().format(Format.SVG_STANDALONE);
		assertEquals("{format:'SVG_STANDALONE',engine:'dot'}", options.toJson(true));
		assertEquals("{format:'svg',engine:'dot'}", options.toJson(false));

		String render = "render('" + dot + "'," + options.toJson(false) + ");";
		String result = engine.execute(render, options); //We also have to give options here to make sure SVG_STANDALONE is used
		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.svg_standalone");

		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowDot2SVGCustomOptions() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		String result = engine.execute(dot, Options.create().format(Format.SVG));
		assertNotNull(result);

		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	// This should be the last test case to run since it changes the graphviz version(prepend 'z' to method name)
	@Test
	public void zgetUnknownVizJsVersion() throws Exception {
		IOException e = assertThrows(IOException.class, () -> new GraphvizEngine("1.2.3"));
		assertEquals("failed to open vizjs file for version [1.2.3]", e.getMessage());
	}

	@Test
	public void getFaultyDot() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		assertThrows(FlowGenerationException.class, () -> engine.execute("i'm not a dot!"));
		engine.close();
	}

	@Test
	public void smallerFontSize() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create();
		options.fontAdjust(0.5);
		String result = engine.execute(dot, options);
		assertNotNull(result);

		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.0.5.svg");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void largerFontSize() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create();
		options.fontAdjust(1.5);
		String result = engine.execute(dot, options);
		assertNotNull(result);

		URL svg = ClassLoaderUtils.getResourceURL(scopeProvider, "flow.1.5.svg");
		assertNotNull(svg);
		TestAssertions.assertEqualsIgnoreWhitespaces(StreamUtil.streamToString(svg.openStream()), result);
		engine.close();
	}
}
