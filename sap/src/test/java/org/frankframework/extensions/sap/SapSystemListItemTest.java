package org.frankframework.extensions.sap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.util.SapSystemListItem;

public class SapSystemListItemTest {

	@BeforeEach
	public void setUp() {
		SapSystemListItem.clear();
	}

	@Test
	public void testSapSystemIsRegistered() {
		//Arrange
		var sapSystem1 = new SapSystem();
		sapSystem1.setName("sapSystem1");

		//Act
		SapSystemListItem.registerItem(sapSystem1);

		//Assert
		assertEquals(1, SapSystemListItem.getRegisteredNamesAsList().size());
	}

}
