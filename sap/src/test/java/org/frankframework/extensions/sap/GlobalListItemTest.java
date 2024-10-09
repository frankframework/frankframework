package org.frankframework.extensions.sap;

import org.frankframework.util.GlobalListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalListItemTest {

	@BeforeEach
	public void setUp() {
		GlobalListItem.clear();
	}

	@Test
	public void testSapSystemIsRegistered() {
		//Arrange
		var sapSystem1 = new SapSystem();
		sapSystem1.setName("sapSystem1");

		//Act
		sapSystem1.registerItem(null);

		//Assert
		assertEquals(1, GlobalListItem.getRegisteredNamesAsList().size());
	}

}
