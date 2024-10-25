package org.frankframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.SenderPipe;
import org.frankframework.senders.ParallelSenders;
import org.frankframework.senders.SenderSeries;
import org.frankframework.senders.XsltSender;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.TransformerPool.OutputType;

public class ParallelXsltTest extends XsltErrorTestBase<SenderPipe> {

	public int NUM_SENDERS=10;
	private List<XsltSender> xsltSenders;
	boolean expectExtraParamWarning=false;

	@BeforeEach
	public void clear() {
		expectExtraParamWarning=false;
	}

	public static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of("classic", false, false, false),
				Arguments.of("new, no stream", true, false, false),
				Arguments.of("output to stream", true, false, true)  // no stream providing, cannot be done in parallel
		);
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
			psenders.addSender(sender);
			xsltSenders.add(sender);
		}
		session.put("sessionKeyGlobal","sessionKeyGlobalValue");
		psenders.addParameter(ParameterBuilder.create().withName("sessionKeyGlobal").withSessionKey("sessionKeyGlobal"));
		pipe.setSender(psenders);
		return pipe;
	}

	@AfterEach
	@Override
	public void tearDown() {
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

		/* Parallel sender uses toXml method which escapes the new line char. In the comparison we need unescaped char.*/
		actual = actual.replace("&#xA;", "&#10;").replace("WindowsPath", "IGNORE").replace("UnixPath", "IGNORE").replace("byte[]", "IGNORE");
		if (stripAllWhitespace) {
			super.assertResultsAreCorrect(combinedExpected.toString().replaceAll("\\s",""), actual.replaceAll("\\s",""), session);
		} else {
			super.assertResultsAreCorrect(combinedExpected.toString(), actual, session);
		}
	}

	@Override
	protected void checkTestAppender(int expectedSize, String expectedString, TestAppender appender) {
		super.checkTestAppender(expectedSize+(expectExtraParamWarning?1:0),expectedString, appender);
		if (expectExtraParamWarning) assertThat(appender.toString(),containsString("are not available for use by nested Senders"));
	}

	@Override
	@Test
	@Disabled("test fails in parallel, ParallelSenders does not propagate exception")
	public void documentIncludedInSourceNotFoundXslt2() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlBasic() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlNoMethodConfigured() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void anyXmlIndent() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, results get escaped")
	public void anyXmlAsText() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void skipEmptyTagsXslt1() {
		// test is ignored
	}

	@Override
	@Test
	@Disabled("test fails in parallel, processing instructions are ignored by XmlBuilder in ParallelSenders")
	public void skipEmptyTagsXslt2() {
		// test is ignored
	}

	@Test
	@Override
	@Disabled("test fails in parallel, parameters are not passed to the individual parallel senders")
	public void xPathFromParameter() {
		// test is ignored
	}

	@Override
	protected int getMultiplicity() {
		return NUM_SENDERS;
	}

	@Test
	@Override
	public void duplicateImportErrorAlertsXslt1() throws Exception {
		expectExtraParamWarning=true;
		super.duplicateImportErrorAlertsXslt1();
	}

	@Test
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
	protected void setXsltVersion(int xsltVersion) {
		for (XsltSender sender : xsltSenders) {
			sender.setXsltVersion(xsltVersion);
		}
	}

	@Override
	protected void setHandleLexicalEvents(boolean handleLexicalEvents) {
		for (XsltSender sender:xsltSenders) {
			sender.setHandleLexicalEvents(handleLexicalEvents);
		}
	}

}
