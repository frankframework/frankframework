package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.frankframework.parameters.NumberParameter;

import org.frankframework.util.SpringUtils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

@Tag("mytag")
public class DelayPipeTest extends PipeTestBase<DelayPipe> {

	@Override
	public DelayPipe createPipe() {
		return SpringUtils.createBean(getConfiguration(), DelayPipe.class);
	}

	@Test
	public void getterSetterDelayTime() {
		long dummyTime = 1337;
		pipe.setDelayTime(dummyTime);
		assertEquals(pipe.getDelayTime(), dummyTime);
	}

	@Test
	public void testUnInterruptedSession() throws Exception {
		Object input = "dummyInput";

		pipe.setDelayTime(1000);


		session.put("delayTimeValue", 3000L);
		var parameter = new NumberParameter();
		parameter.setName("delayTime");
		parameter.setSessionKey("delayTimeValue");
		pipe.addParameter(parameter);

//		pipe.addParameter(NumberParameterBuilder.create("delayTime", 2000L));

		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertEquals(input, result);

		assertEquals(3000L, pipe.getDelayTime());
	}
}
