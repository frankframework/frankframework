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
package nl.nn.adapterframework.extensions.graphviz;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreWhitespaces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

public class GraphvizEngineTest {

	private String dot = "digraph { a -> b[label=\"0.2\",weight=\"0.2\"]; }";
	private ClassLoader classLoader = this.getClass().getClassLoader();

	@Test
	public void canInitDefaultWithoutErrors() throws IOException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
	}

	@Test
	public void canInitNullWithoutErrors() throws IOException {
		GraphvizEngine engine = new GraphvizEngine(null);
		assertNotNull(engine);
	}

	@Test
	public void happyFlowDot2SVG() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String result = engine.execute(dot);
		URL svg = ClassUtils.getResourceURL(classLoader, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test
	public void happyFlowRender2SVG() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String render = "render('" + dot + "'," + Options.create().toJson(false) + ");";
		String result = engine.execute(render);
		URL svg = ClassUtils.getResourceURL(classLoader, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test
	public void happyFlowDot2SVG_STANDALONE() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		String result = engine.execute(dot, Options.create().format(Format.SVG_STANDALONE));
		URL svg = ClassUtils.getResourceURL(classLoader, "flow.svg_standalone");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test
	public void happyFlowRender2SVG_STANDALONE() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create().format(Format.SVG_STANDALONE);
		assertEquals("{format:'SVG_STANDALONE',engine:'dot'}", options.toJson(true));
		assertEquals("{format:'svg',engine:'dot'}", options.toJson(false));

		String render = "render('" + dot + "'," + options.toJson(false) + ");";
		String result = engine.execute(render, options); //We also have to give options here to make sure SVG_STANDALONE is used
		URL svg = ClassUtils.getResourceURL(classLoader, "flow.svg_standalone");

		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test
	public void happyFlowDot2SVGCustomOptions() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		String result = engine.execute(dot, Options.create().format(Format.SVG));
		assertNotNull(result);

		URL svg = ClassUtils.getResourceURL(classLoader, "flow.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test(expected = IOException.class)
	public void getUnknownVizJsVersion() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine("1.2.3");
		assertNotNull(engine);
		engine.execute(dot);
	}

	@Test(expected = GraphvizException.class)
	public void getFaultyDot() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);
		engine.execute("i'm not a dot!");
	}

	@Test
	public void smallerFontSize() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create();
		options.fontAdjust(0.5);
		String result = engine.execute(dot, options);
		assertNotNull(result);

		URL svg = ClassUtils.getResourceURL(classLoader, "flow.0.5.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}

	@Test
	public void largerFontSize() throws IOException, GraphvizException {
		GraphvizEngine engine = new GraphvizEngine();
		assertNotNull(engine);

		Options options = Options.create();
		options.fontAdjust(1.5);
		String result = engine.execute(dot, options);
		assertNotNull(result);

		URL svg = ClassUtils.getResourceURL(classLoader, "flow.1.5.svg");
		assertNotNull(svg);
		assertEqualsIgnoreWhitespaces(Misc.streamToString(svg.openStream()), result);
	}
}
