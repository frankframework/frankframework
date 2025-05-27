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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Options {
	private static final Pattern FORMAT_PATTERN = Pattern.compile("format:'(.*?)'");
	private static final Pattern MEMORY_PATTERN = Pattern.compile("totalMemory:'(.*?)'");
	private static final Pattern Y_INVERT_PATTERN = Pattern.compile("yInvert:(.*?)");

	private Format format;
	private Integer totalMemory;
	private Boolean yInvert;
	private double fontAdjust = 1;

	private Options(Format format, Integer totalMemory, Boolean yInvert) {
		this.format = format;
		this.totalMemory = totalMemory;
		this.yInvert = yInvert;
	}

	public static Options create() {
		return new Options(Format.SVG, null, null);
	}

	public static Options fromJson(String json) {
		final Matcher format = FORMAT_PATTERN.matcher(json);
		format.find();
		final Matcher memory = MEMORY_PATTERN.matcher(json);
		final boolean hasMemory = memory.find();
		final Matcher yInvert = Y_INVERT_PATTERN.matcher(json);
		final boolean hasYInvert = yInvert.find();
		return new Options(
				Format.valueOf(format.group(1)),
				hasMemory ? Integer.parseInt(memory.group(1)) : null,
				hasYInvert ? Boolean.parseBoolean(yInvert.group(1)) : null);
	}

	public Options format(Format format) {
		this.format = format;
		return new Options(format, totalMemory, yInvert);
	}

	public Options totalMemory(Integer totalMemory) {
		this.totalMemory = totalMemory;
		return new Options(format, totalMemory, yInvert);
	}

	public Options yInvert(Boolean yInvert) {
		this.yInvert = yInvert;
		return new Options(format, totalMemory, yInvert);
	}

	public Options fontAdjust(double fontAdjust) {
		this.fontAdjust = fontAdjust;
		return new Options(format, totalMemory, yInvert);
	}

	public String postProcess(String result) {
		return format.postProcess(result, fontAdjust);
	}

	public String toJson(boolean raw) {
		final String form = "format:'" + (raw ? format : format.vizName) + "'";
		final String eng = ",engine:'dot'";
		final String mem = totalMemory == null ? "" : (",totalMemory:'" + totalMemory + "'");
		final String yInv = yInvert == null ? "" : (",yInvert:" + yInvert);
		return "{" + form + eng + mem + yInv + "}";
	}

	@Override
	public String toString() {
		return super.toString()+" - engine[dot] format["+format+"] memory["+totalMemory+"] yInvert["+yInvert+"]";
	}
}
