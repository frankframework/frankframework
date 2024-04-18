package org.frankframework.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecordTransformerTest {

	@Test
	void testAlignForValLengthLeftAlignFillchar() {
		String s = "test";
		String res1 = RecordTransformer.align(s, 10, true, 'b');
		String res2 = RecordTransformer.align(s, 2, true, 'b');
		String res3 = RecordTransformer.align(s, 4, false, 'b');
		assertEquals("testbbbbbb", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testAlignForValLengthRightAlignFillchar() {
		String s = "test";
		String res1 = RecordTransformer.align(s, 10, false, 'c');
		String res2 = RecordTransformer.align(s, 2, false, 'c');
		String res3 = RecordTransformer.align(s, 4, false, 'c');
		assertEquals("cccccctest", res1);
		assertEquals("te", res2);
		assertEquals("test", res3);
	}

	@Test
	void testGetFilledArray() {
		char[] arr = RecordTransformer.getFilledArray(5, 'a');
		assertEquals("aaaaa", new String(arr));
	}

}
