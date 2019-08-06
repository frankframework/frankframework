package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public class J2V8CallbackTest extends SenderTestBase<JavascriptSender> {

    private IPipeLineSession session = new PipeLineSessionBase();
    
    @Rule
	public ExpectedException exception = ExpectedException.none();

    @Override
    public JavascriptSender createSender() {
        return new JavascriptSender();
    }
    
    //A LogSender will be called in the javascript code.
    @Test
    public void LogWithSender() throws ConfigurationException, SenderException, TimeOutException {
        String dummyInput = "dummyinput";
        sender.setjsFileName("Javascript/JavascriptTest.js"); 
        sender.setjsFunctionName("f4");
        sender.setengineName("J2V8");
        
        ParameterResolutionContext prc = new ParameterResolutionContext(dummyInput, session);
        
        Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("3");
		sender.addParameter(param);
	
		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("4");
		sender.addParameter(param2);
		
        LogSender log = new LogSender();
        log.setName("log");
        sender.setSender(log);
        
		sender.configure();
		sender.open();
		
        assertEquals("7", sender.sendMessage(null,dummyInput,prc));
    }
    
    //A FileSender will be called in the javascript function to write something to a new file and afterwards delete the file.
    @Test
    public void WriteFileWithSender() throws ConfigurationException, SenderException, TimeOutException {
        String dummyInput = "dummyinput";
        sender.setjsFileName("Javascript/JavascriptTest.js"); 
        sender.setjsFunctionName("f5");
        sender.setengineName("J2V8");
        
        ParameterResolutionContext prc = new ParameterResolutionContext(dummyInput, session);
        
        Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("3");
		sender.addParameter(param);
	
		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("4");
		sender.addParameter(param2);
		
        FileSender file = new FileSender();
        file.setDirectory("./src/test/resources/Javascript");
        file.setActions("write_append, delete");
        file.setName("file");
        file.setFileName("WriteFile.txt");
        sender.setSender(file);
        
		sender.configure();
		sender.open();
		
        assertEquals("FileSender", sender.sendMessage(null,dummyInput,prc));
    }
    
}