package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.UUIDGeneratorPipe.Type;

public class UUIDGeneratorPipeTest extends PipeTestBase<UUIDGeneratorPipe> {

	private final Object input = new Object();

	@Override
	public UUIDGeneratorPipe createPipe() {
		return new UUIDGeneratorPipe();
	}

	@Test
	public void testTypeIsNormal() throws ConfigurationException {
		pipe.setType(Type.NUMERIC);
		pipe.configure();
	}

	@Test
	public void checkResultNotRightType() throws Exception {
		pipe.setType(Type.NUMERIC);
		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertNotNull(result);
		assertEquals(result.length(), 31);
	}

	@Test
	public void checkResultRightType() throws Exception {
		pipe.setType(Type.ALPHANUMERIC);
		pipe.configure();
		pipe.start();
		PipeRunResult first = doPipe(pipe, input, session);
		PipeRunResult second = doPipe(pipe, input, session);

		String resultFirst = first.getResult().asString();
		String resultSecond = second.getResult().asString();

		assertEquals(resultFirst.length(), resultSecond.length());
	}

}
