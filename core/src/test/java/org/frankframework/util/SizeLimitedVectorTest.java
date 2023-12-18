package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SizeLimitedVectorTest {

	@Test
	public void testAdd() {
		SizeLimitedVector<String> slv = new SizeLimitedVector<>(10);
		slv.add("testString");
		assertEquals(10, slv.getMaxSize());
		assertEquals("testString", slv.get(0));

	}

	@Test
	public void testMaxSizePassed() throws ArrayIndexOutOfBoundsException {
		SizeLimitedVector<Integer> slv = new SizeLimitedVector<>(1);
		slv.add(13);
		slv.add(14);
		assertEquals(14, slv.get(0));
	}

	@Test
	public void testSetMaxSize() {
		SizeLimitedVector<Integer> slv = new SizeLimitedVector<>();
		slv.setMaxSize(6);
		slv.add(1);
		slv.add(2);
		slv.add(3);
		slv.add(4);
		slv.add(5);
		slv.add(6);
		slv.add(7);
		assertEquals(6, slv.getMaxSize());
		assertEquals(10, slv.capacity());
		assertEquals(6, slv.size());
		assertEquals(2, slv.get(0));
	}
}
