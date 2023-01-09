package nl.nn.adapterframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedResultPipe;
import nl.nn.adapterframework.stream.Message;

public class InputOutputPipeProcessorTest {

	private InputOutputPipeProcessor processor;
	private PipeLine pipeLine;
	private PipeLineSession session; 
	
	@BeforeEach
	public void setUp() {
		processor = new InputOutputPipeProcessor();
		PipeProcessor chain = new PipeProcessor() {
			@Override
			public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
				return pipe.doPipe(message, pipeLineSession);
			}

			@Override
			public PipeRunResult validate(PipeLine pipeLine, IValidator validator, Message message, PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
				return validator.validate(message, pipeLineSession, messageRoot);
			}
		};
		processor.setPipeProcessor(chain);
		
		pipeLine = new PipeLine();
		Adapter owner = new Adapter();
		owner.setName("PipeLine owner");
		pipeLine.setOwner(owner);
		
		session = new PipeLineSession();
	}

	public void testRestoreMovedElement(Object sessionVarContents) throws Exception {
		
		FixedResultPipe pipe = new FixedResultPipe();
		pipe.setRestoreMovedElements(true);
		pipe.setReturnString("result [{sessionKey:replaceThis}]");
		PipeForward forward = new PipeForward();
		forward.setName("success");
		pipe.registerForward(forward);
		pipe.configure();
		pipe.start();
		
		Message input = new Message("input");
		
		session.put("replaceThis", sessionVarContents);
				
		
		PipeRunResult prr = processor.processPipe(pipeLine, pipe, input, session);
		
		assertEquals("result [ReplacedValue]", prr.getResult().asString());
		
	}

	@Test
	public void testRestoreMovedElementString() throws Exception {
		testRestoreMovedElement("ReplacedValue");
	}

	@Test
	public void testRestoreMovedElementMessage() throws Exception {
		testRestoreMovedElement(new Message("ReplacedValue"));
	}

	@Test
	public void testRestoreMovedElementReader() throws Exception {
		testRestoreMovedElement(new StringReader("ReplacedValue"));
	}

	@Test
	public void testRestoreMovedElementByteArray() throws Exception {
		testRestoreMovedElement("ReplacedValue".getBytes());
	}
}
