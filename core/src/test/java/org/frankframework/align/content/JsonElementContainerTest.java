package org.frankframework.align.content;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;


public class JsonElementContainerTest {


	public void testStripLeadingZeroes(String expected, String input) {
		String actual= JsonElementContainer.stripLeadingZeroes(input);
		assertEquals(expected,actual);
	}

	@Test
	public void testStripLeadingZeroes() {
		for (int i=1; i<12;i++) {
			testStripLeadingZeroes(""+i,""+i);
			testStripLeadingZeroes(""+i,"0"+i);
			testStripLeadingZeroes("0.0"+i,"0.0"+i);
			testStripLeadingZeroes("0.0"+i,"000.0"+i);
			testStripLeadingZeroes("-"+i,"-"+i);
			testStripLeadingZeroes("-"+i,"-0"+i);
			testStripLeadingZeroes("-0.0"+i,"-0.0"+i);
			testStripLeadingZeroes("-0.0"+i,"-000.0"+i);
		}
		testStripLeadingZeroes("0","0");
		testStripLeadingZeroes("0","00");
		testStripLeadingZeroes("0","000");
	}

}
