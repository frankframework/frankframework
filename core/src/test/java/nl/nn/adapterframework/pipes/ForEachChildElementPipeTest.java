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

	private String messageBasic="<root><sub>abc</sub><sub>def</sub></root>";
	private String expectedBasic="<results count=\"2\">\n"+
			"<result item=\"1\">\n"+
			"abc\n"+
			"</result>\n"+
			"<result item=\"2\">\n"+
			"def\n"+
			"</result>\n</results>";

	private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public ForEachChildElementPipe createPipe() {
        return new ForEachChildElementPipe();
    }

    protected ISender getElementRenderer() {
    	XsltSender sender = new XsltSender();
    	sender.setXpathExpression("sub");
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
    	pipe.setElementXPathExpression("/root/sub");
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
    	pipe.setElementXPathExpression("/root/sub[position()<3]");
    	pipe.setNamespaceAware(true);
    	pipe.setSender(getElementRenderer());
    	configurePipe();
    	pipe.start();

        PipeRunResult prr = pipe.doPipe(messageBasic, session);
        String actual=prr.getResult().toString();
        
        assertEquals(expectedBasic, actual);
    }

}