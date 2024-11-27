package org.frankframework.extensions.bis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class BisUtilsTest {

	@Test
	public void testListToStringWithStringList() {
		List<String> list = new ArrayList<>();
		list.add("bailar");
		list.add("besos");
		String res = BisUtils.listToString(list);
		assertEquals("bailarbesos", res);
	}
}
