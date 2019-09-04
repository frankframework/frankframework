package nl.nn.adapterframework.xslt;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
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
		expected=stripPrefix(expected, xmlPrefix);
		expected=stripPrefix(expected, xmlPrefix.replaceAll("\\s",""));

		String combinedExpected="<results>";
	
		for (int i=0;i<NUM_SENDERS;i++) {
			combinedExpected+="\n<result senderClass=\"XsltSender\" type=\"String\">"
					+expected.replaceFirst(">headerDefault<", ">header"+i+"<")
							 .replaceFirst(">sessionKeyDefault<", ">sessionKeyValue"+i+"<")
							 //.replaceFirst(">sessionKeyGlobalDefault<", ">sessionKeyGlobalValue<")
							 +"\n</result>";
		}
		combinedExpected+="\n</results>";
//		super.assertResultsAreCorrect(
//				combinedExpected.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"), 
//						  actual.replaceAll("\\r\\n","\n").replaceAll("  ","").replaceAll("\\n ","\n"), session);

//		super.assertResultsAreCorrect(combinedExpected, actual, session);

		super.assertResultsAreCorrect(
		combinedExpected.replaceAll("\\s",""), 
				  actual.replaceAll("\\s",""), session);
	}

	@Override
	protected void checkTestAppender(int expectedSize, String expectedString) {
		super.checkTestAppender(expectedSize+1,expectedString);
		assertThat(testAppender.toString(),containsString("are not available for use by nested Senders"));
	}

	
	@Override
	protected int getMultiplicity() {
		return NUM_SENDERS;
	}

	@Override
	@Test
    @Ignore("configwarning test fails when run in parallel, won't fix")
	public void testConfigWarnings() {
		fail("Test should have been ignored");
	}
	@Override
	@Test
    @Ignore("error handling is different in parallel")
	public void documentNotFoundXslt1() throws Exception {
		fail("Test should have been ignored");
	}
	@Override
	@Test
    @Ignore("error handling is different in parallel")
	public void documentNotFoundXslt2() throws Exception {
		fail("Test should have been ignored");
	}
	
	@Override
	protected void setStyleSheetName(String styleSheetName) {
		for (XsltSender sender:xsltSenders) {
			sender.setStyleSheetName(styleSheetName);	
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
