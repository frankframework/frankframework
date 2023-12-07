package nl.nn.adapterframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.SenderPipe;
import nl.nn.adapterframework.senders.ParallelSenders;
import nl.nn.adapterframework.senders.SenderSeries;
import nl.nn.adapterframework.senders.XsltSender;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class ParallelXsltTest extends XsltErrorTestBase<SenderPipe> {

	public int NUM_SENDERS=10;
	private List<XsltSender> xsltSenders;
	boolean expectExtraParamWarning=false;

	@Before
	public void clear() {
		expectExtraParamWarning=false;
	}

	@Parameters(name = "{index}: {0}: provide [{2}] stream out [{3}]")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				 { "classic", 			false, false, false },
				 { "new, no stream", 	 true, false, false },
				 { "output to stream", 	 true, false, true  }  // no stream providing, cannot be done in parallel
		});
	}


	protected SenderSeries createSenderContainer() {
		SenderSeries senders=new ParallelSenders();
		autowireByType(senders);
		return senders;
	}

	@Override
	public SenderPipe createPipe() {
		SenderPipe pipe = new SenderPipe();
		SenderSeries psenders=createSenderContainer();
		xsltSenders=new ArrayList<>();
		for(int i=0;i<NUM_SENDERS;i++) {
			XsltSender sender = new XsltSender();
			//sender.setSessionKey("out"+i);
			sender.setOmitXmlDeclaration(true);

			sender.addParameter(new Parameter("header", "header"+i));

			session.put("sessionKey"+i,"sessionKeyValue"+i);
			sender.addParameter(ParameterBuilder.create().withName("sessionKey").withSessionKey("sessionKey"+i));

			autowireByType(sender);
			psenders.registerSender(sender);
			xsltSenders.add(sender);
		}
		session.put("sessionKeyGlobal","sessionKeyGlobalValue");
		psenders.addParameter(ParameterBuilder.create().withName("sessionKeyGlobal").withSessionKey("sessionKeyGlobal"));
		pipe.setSender(psenders);
		return pipe;
	}

	@After
	@Override
	public void tearDown() throws Exception {
		xsltSenders = null;
		super.tearDown();
	}

	private String stripPrefix(String string, String prefix) {
		if (string.startsWith(prefix)) {
			string=string.substring(prefix.length());
		}
		return string;
	}

	@Override
	protected void assertResultsAreCorrect(String expected, String actual, PipeLineSession session) {
		String xmlPrefix="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		boolean stripAllWhitespace=true; // to cope with differences between unix and windows line endings

		expected=stripPrefix(expected, xmlPrefix);
		expected=stripPrefix(expected, xmlPrefix.replaceAll("\\s",""));

		StringBuilder combinedExpected= new StringBuilder("<results>");

		for (int i = 0; i < NUM_SENDERS; i++) {
			combinedExpected
					.append("<result senderClass=\"XsltSender\" success=\"true\" type=\"IGNORE\">")
					.append(expected
									.replaceFirst(">headerDefault<", ">header" + i + "<")
									.replaceFirst(">sessionKeyDefault<", ">sessionKeyValue" + i + "<")
							//.replaceFirst(">sessionKeyGlobalDefault<", ">sessionKeyGlobalValue<")
					).append("</result>");
		}
		combinedExpected.append("</results>");
//		super.assertResultsAreCorrect(
//				combinedExpected.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"),
//						  actual.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"), session);

//		super.assertResultsAreCorrect(combinedExpected, actual, session);

		/* Parallel sender uses toXml method which escapes the new line char. In the comparison we need unescaped char.*/
		actual = actual.replace("&#xA;", "&#10;").replace("WindowsPath", "IGNORE").replace("UnixPath", "IGNORE");
		if (stripAllWhitespace) {
			super.assertResultsAreCorrect(combinedExpected.toString().replaceAll("\\s",""), actual.replaceAll("\\s",""), session);
		} else {
			super.assertResultsAreCorrect(combinedExpected.toString(), actual, session);
		}
	}

	@Override
	protected void checkTestAppender(int expectedSize, String expectedString) {
		super.checkTestAppender(expectedSize+(expectExtraParamWarning?1:0),expectedString);
		if (expectExtraParamWarning) assertThat(testAppender.toString(),containsString("are not available for use by nested Senders"));
	}

	@Override
	@Ignore("test fails in parallel, ParallelSenders does not propagate exception")
	public void documentIncludedInSourceNotFoundXslt2() throws Exception {
		// test is ignored
	}

	@Override
	@Ignore("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlBasic() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlNoMethodConfigured() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlIndent() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, results get escaped")
	public void anyXmlAsText() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void skipEmptyTagsXslt1() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void skipEmptyTagsXslt2() throws Exception {
		// test is ignored
	}
	@Override
	@Ignore("test fails in parallel, parameters are not passed to the individual parallel senders")
	public void xPathFromParameter() throws Exception {
		// test is ignored
	}

	@Override
	protected int getMultiplicity() {
		return NUM_SENDERS;
	}

	@Override
	public void duplicateImportErrorAlertsXslt1() throws Exception {
		expectExtraParamWarning=true;
		super.duplicateImportErrorAlertsXslt1();
	}
	@Override
	public void duplicateImportErrorAlertsXslt2() throws Exception {
		assumeFalse(TestAssertions.isTestRunningOnGitHub()); // test fails on GitHub, with two extra alerts in logging. So be it.
		expectExtraParamWarning=true;
		super.duplicateImportErrorAlertsXslt2();
	}


	@Override
	protected void setStyleSheetName(String styleSheetName) {
		for (XsltSender sender:xsltSenders) {
			sender.setStyleSheetName(styleSheetName);
		}
	}

	@Override
	protected void setStyleSheetNameSessionKey(String styleSheetNameSessionKey) {
		for (XsltSender sender:xsltSenders) {
			sender.setStyleSheetNameSessionKey(styleSheetNameSessionKey);
		}
	}

	@Override
	protected void setXpathExpression(String xpathExpression) {
		for (XsltSender sender:xsltSenders) {
			sender.setXpathExpression(xpathExpression);
		}
	}

	@Override
	protected void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
		for (XsltSender sender:xsltSenders) {
			sender.setOmitXmlDeclaration(omitXmlDeclaration);
		}
	}

	@Override
	protected void setIndent(boolean indent) {
		for (XsltSender sender:xsltSenders) {
			sender.setIndentXml(indent);
		}
	}

	@Override
	protected void setSkipEmptyTags(boolean skipEmptyTags) {
		for (XsltSender sender:xsltSenders) {
			sender.setSkipEmptyTags(skipEmptyTags);
		}
	}

	@Override
	protected void setOutputType(OutputType outputType) {
		for (XsltSender sender:xsltSenders) {
			sender.setOutputType(outputType);
		}
	}


	@Override
	protected void setRemoveNamespaces(boolean removeNamespaces) {
		for (XsltSender sender:xsltSenders) {
			sender.setRemoveNamespaces(removeNamespaces);
		}
	}

	@Override
	protected void setXslt2(boolean xslt2) {
		for (XsltSender sender : xsltSenders) {
			sender.setXsltVersion(xslt2 ? 2 : 1);
		}
	}

	@Override
	protected void setHandleLexicalEvents(boolean handleLexicalEvents) {
		for (XsltSender sender:xsltSenders) {
			sender.setHandleLexicalEvents(handleLexicalEvents);
		}
	}

}
