package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SpecificParameterTest {
	@Test
	public void whenParamTagHasNoSpaceThenOnlyName() {
		SpecificParameter p = SpecificParameter.getInstance("myName");
		assertNull(p.getDescription());
		assertEquals("myName", p.getName());
	}

	@Test
	public void whenParamTagHasSpacesAfterNameThenStillDescriptionNull() {
		SpecificParameter p = SpecificParameter.getInstance("myName ");
		assertNull(p.getDescription());
		assertEquals("myName", p.getName());		
	}

	@Test
	public void whenParamTagHasMultipleWordsThenNameAndDescription() {
		SpecificParameter p = SpecificParameter.getInstance("myName        Description ");
		assertEquals("myName", p.getName());
		assertEquals("Description", p.getDescription());
	}
}
