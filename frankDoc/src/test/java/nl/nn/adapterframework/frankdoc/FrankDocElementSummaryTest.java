package nl.nn.adapterframework.frankdoc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;

public class FrankDocElementSummaryTest {
	private static final String PACKAGE = "nl.nn.adapterframework.frankdoc.testtarget.element.summary.";
	private static final String DIGESTER_RULES_FILE_NAME = "general-test-digester-rules.xml";
	private static final String EXPECTED = Arrays.asList(
		"              Master: Master",
		"              Object: ", 
		"    Other (from sub): ", 
		"Other (from summary): ").stream().map(s -> s + "\n").collect(Collectors.joining());
	
	@Test
	public void testElementSummary() throws IOException {
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(PACKAGE);
		FrankDocModel model = FrankDocModel.populate(getDigesterRulesInputSource(DIGESTER_RULES_FILE_NAME), PACKAGE + "Master", classRepository);
		FrankDocElementSummaryFactory instance = new FrankDocElementSummaryFactory(model);
		String actual = instance.getText();
		System.out.println(actual);
		assertEquals(EXPECTED, actual);
	}

	private InputSource getDigesterRulesInputSource(String fileName) throws IOException {
		return FrankDocModel.openResource("doc/" + fileName);
	}

}
