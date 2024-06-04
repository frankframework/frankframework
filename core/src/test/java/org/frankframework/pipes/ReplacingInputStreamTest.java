package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by simon on 8/29/17.
 * Copyright 2017 Simon Haoran Liang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class ReplacingInputStreamTest {
	private ByteArrayInputStream bis;
	private ReplacingInputStream ris;
	private ByteArrayOutputStream bos;

	@BeforeEach
	public void beforeTest() throws Exception {
		byte[] bytes = "hello xyz world.".getBytes("UTF-8");
		bis = new ByteArrayInputStream(bytes);
	}

	@Test
	public void testReplacingInputStream() throws Exception {
		ris = new ReplacingInputStream(bis, "xyz", "abc", false, null, false);
		bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = ris.read()))
			bos.write(b);

		assertEquals("hello abc world.", bos.toString());
	}

	@Test
	public void testReplacingToEmptyString() throws Exception {
		ris = new ReplacingInputStream(bis, "xyz", "", false, null, false);
		bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = ris.read()))
			bos.write(b);

		assertEquals("hello  world.", bos.toString());
	}

	@Test
	public void testWithEmptySearchAndReplace() throws Exception {
		ris = new ReplacingInputStream(bis, "", "", false, null, false);
		bos = new ByteArrayOutputStream();

		int b;
		while (-1 != (b = ris.read()))
			bos.write(b);

		assertEquals("hello xyz world.", bos.toString());
	}
}
