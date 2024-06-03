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
package org.frankframework.util.flow.graphviz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FormatTest {

	String dummy = "font-size=\""+10+"\"";

	@Test
	void svgTest() {
		Format format = Format.SVG;
		assertEquals("svg", format.fileExtension);
		assertFalse(format.image);
		assertTrue(format.svg);
		assertEquals("svg", format.vizName);
	}

	@Test
	void randomInputHalfTheSize() {
		Format format = Format.SVG;
		assertEquals("test123", format.postProcess("test123", 0.5));
	}

	@Test
	void halfTheSizeAsSvg() {
		Format format = Format.SVG;
		assertEquals("font-size=\"5.0\"", format.postProcess(dummy, 0.5));
	}

	@Test
	void equalSizeAsSvg() {
		Format format = Format.SVG;
		assertEquals("font-size=\"10\"", format.postProcess(dummy, 1.0));
	}

	@Test
	void doubleTheSizeAsSvg() {
		Format format = Format.SVG;
		assertEquals("font-size=\"20.0\"", format.postProcess(dummy, 2.0));
	}

	@Test
	void halfTheSizeAsSvgStandalone() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"5.0\"", format.postProcess(dummy, 0.5));
	}

	@Test
	void equalSizeAsSvgStandalone() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"10\"", format.postProcess(dummy, 1.0));
	}

	@Test
	void doubleTheSizeAsSvgStandalone() {
		Format format = Format.SVG_STANDALONE;
		assertEquals("font-size=\"20.0\"", format.postProcess(dummy, 2.0));
	}
}
