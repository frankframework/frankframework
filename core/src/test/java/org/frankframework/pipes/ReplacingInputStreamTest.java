package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Created by simon on 8/29/17.
 * Copyright 2017 Simon Haoran Liang
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class ReplacingInputStreamTest {

	public static Stream<Arguments> testReplacingInputStream() {
		return Stream.of(
				Arguments.of("xyz", "abc", false, "", "hello xyz world.", "hello abc world."),
				Arguments.of("xyz", "", false, "", "hello xyz world.", "hello  world."),
				Arguments.of("", "", false, "", "hello xyz world.", "hello xyz world."),
				Arguments.of("xyz", "xyzxyz", false, "", "hello xyz world.", "hello xyzxyz world."),
				Arguments.of("", "", true, "R", "hello \bxyz world.", "hello Rxyz world.")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void testReplacingInputStream(String search, String replacement, boolean replaceXml, String nonXmlCharReplacement, String input, String expected) throws Exception {
		ReplacingInputStream ris = new ReplacingInputStream(getByteArrayInputStream(input), search, replacement, replaceXml, nonXmlCharReplacement, false);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = ris.read())) {bos.write(b);}

		assertEquals(expected, bos.toString());
	}

	private ByteArrayInputStream getByteArrayInputStream(String input) {
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}
}
