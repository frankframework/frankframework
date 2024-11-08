package org.frankframework.extensions.sap;

import org.frankframework.util.SapSystemListItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
