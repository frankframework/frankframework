package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.senders.XsltSender;

public class ForEachChildElementPipeTest extends PipeTestBase<ForEachChildElementPipe> {

	private String messageBasic = "<root><num>1</num><num>2</num></root>";
	private String expectedBasic =
			"<results count=\"2\">\n"+
			"<result item=\"1\">\n1\n</result>\n"+
			"<result item=\"2\">\n2\n</result>\n" +
			"</results>";
	private String messageExpanded = "<root><num>1</num><num>2</num><num>3</num><num>4</num><num>5</num></root>";
	private String expectedExpanded =
			"<results count=\"3\">\n"+
			"<result item=\"1\">\n2\n</result>\n"+
			"<result item=\"2\">\n3\n</result>\n" +
			"<result item=\"3\">\n4\n</result>\n" +
			"</results>";
	
	private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public ForEachChildElementPipe createPipe() {
        return new ForEachChildElementPipe();
    }

    protected ISender getElementRenderer() {
    	XsltSender sender = new XsltSender();
    	sender.setXpathExpression("num/text()");
    	return sender;
    }

    
    @Test
    public void testBasic() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setSender(getElementRenderer());
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasic, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
    }

    @Test
    public void testXPath() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setElementXPathExpression("/root/num");
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer());
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasic, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
    }

    @Test
    public void testXPathWithSpecialChars() throws PipeRunException, ConfigurationException, PipeStartException {
    	pipe.setElementXPathExpression("/root/num[position()>1 and position()<5]");
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer());
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageExpanded, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedExpanded, actual);
    }

}