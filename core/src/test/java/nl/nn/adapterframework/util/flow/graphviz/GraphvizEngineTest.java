package nl.nn.adapterframework.util.flow.graphviz;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreWhitespaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.flow.FlowGenerationException;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowRender2SVG() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String render = "render('" + dot + "'," + Options.create().toJson(false) + ");";
		String result = engine.execute(render);
		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowDot2SVG_STANDALONE() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String result = engine.execute(dot, Options.create().format(Format.SVG_STANDALONE));
		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.svg_standalone");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
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
		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.svg_standalone");

		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
		engine.close();
	}

	@Test
	public void happyFlowDot2SVGCustomOptions() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		String result = engine.execute(dot, Options.create().format(Format.SVG));
		assertNotNull(result);

		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
		engine.close();
	}
	// This should be the last test case to run since it changes the graphviz version(prepend 'z' to method name)
	@Test(expected = IOException.class)
	public void zgetUnknownVizJsVersion() throws Exception {
		GraphvizEngine engine = new GraphvizEngine("1.2.3");
		assertNotNull(engine);
		engine.execute(dot);
		engine.close();
	}

	@Test(expected = FlowGenerationException.class)
	public void getFaultyDot() throws Exception {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		engine.execute("i'm not a dot!");
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

		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.0.5.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
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

		URL svg = ClassUtils.getResourceURL(scopeProvider, "flow.1.5.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
		engine.close();
	}
}
