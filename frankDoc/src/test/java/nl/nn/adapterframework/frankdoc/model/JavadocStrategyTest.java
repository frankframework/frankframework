package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavadocStrategyTest {
	@Test
	public void whenDescriptionMissesDotSpaceThenDescriptionHeaderIsSame() {
		String description = "This is a sentence.";
		assertEquals("This is a sentence.", JavadocStrategy.calculateDescriptionHeader(description));
	}

	@Test
	public void whenDescriptionMissesDotThenNoExtraDotAdded() {
		String description = "This is a sentence";
		assertEquals("This is a sentence", JavadocStrategy.calculateDescriptionHeader(description));		
	}

	@Test
	public void whenDescriptionHasDotSpaceThenDescriptionHeaderFirstSentenceWithDot() {
		String description = "This is a sentence. This is the second sentence";
		assertEquals("This is a sentence.", JavadocStrategy.calculateDescriptionHeader(description));		
	}

	@Test
	public void whenDescriptionHasDotNewlineThenDescriptionHeaderFirstSentenceWithDot() {
		String description = "This is a sentence.\nThis is the second sentence";
		assertEquals("This is a sentence.", JavadocStrategy.calculateDescriptionHeader(description));		
	}
}
