package nl.nn.adapterframework.frankdoc.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;

public class FrankDocModelDocletTest {
	private static final String SIMPLE = "nl.nn.adapterframework.frankdoc.testtarget.simple.";
	private static final String EXPECTED_DESCRIPTION =
			"The JavaDoc comment of class \"Container\".\n" +
			" \n" +
			" This is additional text that we do not add to the XSDs or the Frank!Doc website.";
	private static final String EXPECTED_DESCRIPTION_HEADER = "The JavaDoc comment of class \"Container\".";

	private FrankDocModel instance;

	@Before
	public void setUp() {
		FrankClassRepository repository = TestUtil.getFrankClassRepositoryDoclet(SIMPLE);
		instance = FrankDocModel.populate("doc/xsd-element-name-digester-rules.xml", SIMPLE + "Container", repository);
	}

	@Test
	public void whenClassHasJavadocThenInFrankElementDescription() {
		FrankElement frankElement = instance.findFrankElement(SIMPLE + "Container");
		assertEquals(EXPECTED_DESCRIPTION, frankElement.getDescription());
		assertEquals(EXPECTED_DESCRIPTION_HEADER, frankElement.getDescriptionHeader());
	}
}
