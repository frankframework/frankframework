package nl.nn.adapterframework.doc;

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
import nl.nn.adapterframework.testutil.TestAssertions;
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
		TestAssertions.assertEqualsIgnoreCRLF(expectedXsd, actualXsd);
	}

	private FrankDocModel createModel() throws Exception {
		return FrankDocModel.populate(
				getDigesterRulesPath(digesterRulesFileName), startClassName);
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
