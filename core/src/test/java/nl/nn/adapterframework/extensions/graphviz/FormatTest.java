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

import static org.junit.Assert.*;

import org.junit.Test;

public class FormatTest {

	public String dummy = "font-size=\""+10+"\"";

	@Test
	public void svgTest() {
		Format format = Format.SVG;
		assertEquals("svg", format.fileExtension);
		assertEquals(false, format.image);
		assertEquals(true, format.svg);
		assertEquals("svg", format.vizName);
	}

	@Test
	public void randomInputHalfTheSize() {
		Format format = Format.SVG;
		assertEquals("test123", format.postProcess("test123", 0.5));
	}

	@Test
	public void halfTheSizeSVG() {
		Format format = Format.SVG;
		assertEquals("font-size=\"5.0\"", format.postProcess(dummy, 0.5));
	}

	@Test
	public void equalSizeSVG() {
		Format format = Format.SVG;
		assertEquals("font-size=\"10\"", format.postProcess(dummy, 1.0));
	}

	@Test
	public void doubleTheSizeSVG() {
		Format format = Format.SVG;
		assertEquals("font-size=\"20.0\"", format.postProcess(dummy, 2.0));
	}

	@Test
	public void halfTheSizeSVG_STANDALONE() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"5.0\"", format.postProcess(dummy, 0.5));
	}

	@Test
	public void equalSizeSVG_STANDALONE() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"10\"", format.postProcess(dummy, 1.0));
	}

	@Test
	public void doubleTheSizeSVG_STANDALONE() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"20.0\"", format.postProcess(dummy, 2.0));
	}
}
