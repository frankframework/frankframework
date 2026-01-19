package org.frankframework.ladybug.tibet2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestTibetView {

	@Test
	public void testTibetView() {
		TibetView view = new TibetView() {

			@Override
			protected String isOpenReportAllowedViaAdapter(String storageId) {
				return storageId;
			}
		};

		// Assert String and Integer values
		assertEquals("one", view.isOpenReportAllowed("one"));
		assertEquals("1", view.isOpenReportAllowed("1"));
		assertEquals("1", view.isOpenReportAllowed(1));
	}
}
