package org.frankframework.util.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FlowDiagramManagerTest {

	@Test
	void testEncodeFileName() {
		assertEquals("_ab__5__c.txt", FlowDiagramManager.encodeFileName(" ab&@5*(c.txt"));
	}

}
