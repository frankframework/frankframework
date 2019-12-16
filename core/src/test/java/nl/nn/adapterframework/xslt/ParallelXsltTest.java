package nl.nn.adapterframework.xslt;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.GenericMessageSendingPipe;
import nl.nn.adapterframework.senders.ParallelSenders;
import nl.nn.adapterframework.senders.SenderSeries;
import nl.nn.adapterframework.senders.XsltSender;

public class ParallelXsltTest extends XsltErrorTestBase<GenericMessageSendingPipe> {
	

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
		SenderSeries senders=new ParallelSenders() {
			@Override
			protected TaskExecutor createTaskExecutor() {
				ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
				taskExecutor.setCorePoolSize(NUM_SENDERS);
				taskExecutor.initialize();
				return taskExecutor;
			}
		};
		return senders;		
	}

	@Override
	public GenericMessageSendingPipe createPipe() {
		GenericMessageSendingPipe pipe = new GenericMessageSendingPipe();
		SenderSeries psenders=createSenderContainer();
		xsltSenders=new ArrayList<XsltSender>();
		for(int i=0;i<NUM_SENDERS;i++) {
			XsltSender sender = new XsltSender();
			//sender.setSessionKey("out"+i);
			sender.setOmitXmlDeclaration(true);
			
			Parameter param1 = new Parameter();
			param1.setName("header");
			param1.setValue("header"+i);			
			sender.addParameter(param1);
			
			Parameter param2 = new Parameter();
			param2.setName("sessionKey");
			param2.setSessionKey("sessionKey"+i);
			session.put("sessionKey"+i,"sessionKeyValue"+i);
			sender.addParameter(param2);
			
			psenders.setSender(sender);
			xsltSenders.add(sender);
		}
		Parameter param = new Parameter();
		param.setName("sessionKeyGlobal");
		param.setSessionKey("sessionKeyGlobal");
		session.put("sessionKeyGlobal","sessionKeyGlobalValue");
		psenders.addParameter(param);
		pipe.setSender(psenders);
		return pipe;
	}

	private String stripPrefix(String string, String prefix) {
		if (string.startsWith(prefix)) {
			string=string.substring(prefix.length());
		}
		return string;
	}
	
	@Override
	protected void assertResultsAreCorrect(String expected, String actual, IPipeLineSession session) {
		String xmlPrefix="<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		boolean stripAllWhitespace=true; // to cope with differences between unix and windows line endings
		
		expected=stripPrefix(expected, xmlPrefix);
		expected=stripPrefix(expected, xmlPrefix.replaceAll("\\s",""));
		
		String combinedExpected="<results>";
	
		for (int i=0;i<NUM_SENDERS;i++) {
			combinedExpected+="<result senderClass=\"XsltSender\" type=\"String\">"
					+expected.replaceFirst(">headerDefault<", ">header"+i+"<")
							 .replaceFirst(">sessionKeyDefault<", ">sessionKeyValue"+i+"<")
							 //.replaceFirst(">sessionKeyGlobalDefault<", ">sessionKeyGlobalValue<")
							 +"</result>";
		}
		combinedExpected+="</results>";
//		super.assertResultsAreCorrect(
//				combinedExpected.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"), 
//						  actual.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"), session);

//		super.assertResultsAreCorrect(combinedExpected, actual, session);

		if (stripAllWhitespace) {
			super.assertResultsAreCorrect(combinedExpected.replaceAll("\\s",""), actual.replaceAll("\\s",""), session);
		} else {
			super.assertResultsAreCorrect(combinedExpected, actual, session);
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
	protected void setOutputType(String outputType) {
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
		for (XsltSender sender:xsltSenders) {
			sender.setXslt2(xslt2);
		}
	}

}
