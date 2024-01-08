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

import org.junit.jupiter.api.Test;

public class OptionsTest {

	@Test
	public void optionsSVGformat() {
		Options options = Options.create();
		options.format(Format.SVG);
		String json = options.toJson(true);

		assertEquals("{format:'SVG',engine:'dot'}", json.trim());
	}

	@Test
	public void optionsSVG_STANDALONEformat() {
		Options options = Options.create();
		options.format(Format.SVG_STANDALONE);
		String json = options.toJson(true);

		assertEquals("{format:'SVG_STANDALONE',engine:'dot'}", json.trim());
	}

	@Test
	public void optionsJSONformat() {
		Options options = Options.create();
		options.format(Format.JSON);
		String json = options.toJson(true);

		assertEquals("{format:'JSON',engine:'dot'}", json.trim());
	}

	@Test
	public void optionsTotalMemory() {
		Options options = Options.create();
		options.totalMemory(512);
		String json = options.toJson(true);

		assertEquals("{format:'SVG',engine:'dot',totalMemory:'512'}", json.trim());
	}

	@Test
	public void optionsYInvertTrue() {
		Options options = Options.create();
		options.yInvert(true);
		String json = options.toJson(true);

		assertEquals("{format:'SVG',engine:'dot',yInvert:true}", json.trim());
	}

	@Test
	public void optionsYInvertFalse() {
		Options options = Options.create();
		options.yInvert(false);
		String json = options.toJson(true);

		assertEquals("{format:'SVG',engine:'dot',yInvert:false}", json.trim());
	}

	@Test
	public void optionsRawJson() {
		Options options = Options.create();
		options.yInvert(false);
		options.totalMemory(512);
		String json = options.toJson(false);

		assertEquals("{format:'svg',engine:'dot',totalMemory:'512',yInvert:false}", json.trim());
	}

	@Test
	public void fullJsonToOptions() {
		String json = "{format:'SVG',engine:'dot',totalMemory:'512',yInvert:false}";
		Options options = Options.fromJson(json);

		assertEquals(json, options.toJson(true).trim());
	}

	@Test
	public void minimalJsonToOptions() {
		String json = "{format:'SVG',engine:'dot'}";
		Options options = Options.fromJson(json);

		assertEquals(json, options.toJson(true).trim());
	}
}
