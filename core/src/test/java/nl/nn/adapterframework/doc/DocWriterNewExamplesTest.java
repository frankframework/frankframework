package nl.nn.adapterframework.doc;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class DocWriterNewExamplesTest {
	@Parameters(name = "{1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"examples-simple-digester-rules.xml", "nl.nn.adapterframework.doc.testtarget.examples.simple.Start", "simple.xsd"}
		});
	}

	@Parameter(0)
	public String digesterRulesFileName;

	@Parameter(1)
	public String startClassName;

	@Parameter(2)
	public String expectedXsdFileName;

	@Test
	public void testXsd() throws Exception {
		FrankDocModel model = createModel();
		DocWriterNew docWriter = new DocWriterNew(model);
		docWriter.init(startClassName);
		String actualXsd = docWriter.getSchema();
		System.out.println(actualXsd);
		String expectedXsd = getExpectedXsd();
		assertEquals(expectedXsd.replace("\r\n", "\n"), actualXsd.replace("\r\n", "\n"));
	}

	private FrankDocModel createModel() throws Exception {
		FrankDocModel result = new FrankDocModel();
		result.createConfigChildDescriptorsFrom(getDigesterRulesPath(digesterRulesFileName));
		result.findFrankElement(startClassName);
		result.setOverriddenFromAndRegisterSyntax1NamesInElementTypes();
		result.buildGroups();
		return result;
	}

	private String getDigesterRulesPath(String fileName) {
		return "doc/" + fileName;
	}

	private String getExpectedXsd() throws Exception {
		String fileName = "doc/examplesExpected/" + expectedXsdFileName;
		Resource resource = Resource.getResource(fileName);
		InputStream is = resource.openStream();
		Reader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		return Misc.readerToString(reader, "\n", false);
	}
}
