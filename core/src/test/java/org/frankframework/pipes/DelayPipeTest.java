package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.ParameterList;

import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Tag("mytag")
public class DelayPipeTest extends PipeTestBase<DelayPipe> {

	@Override
	public DelayPipe createPipe() {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(DelayPipe.class);

		MethodHandler handler = (self, method, proceed, args) -> {
			ValueFromParameter valueFromParameter = method.getAnnotation(ValueFromParameter.class);

			if (valueFromParameter != null) {
				String methodName = method.getName().replaceFirst("^get", "");
				methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);

				final var defaultValue = proceed.invoke(self, args);

				ParameterList parameterList = null;
				for (Method classMethod : self.getClass().getMethods()) {
					if (classMethod.getName().equals("getParameterList")) {
						parameterList = (ParameterList) classMethod.invoke(self);
					}
				}

				if (parameterList == null) {
					return defaultValue;
				}

				try {
					if (!parameterList.hasParameter(methodName)) {
						return defaultValue;
					}

					Message message = Message.asMessage("");
					PipeLineSession s = session;
					final var value = parameterList.getValue(null, parameterList.findParameter(methodName), message, s, true);

					return value.asLongValue((Long) defaultValue);
				} catch (ParameterException e) {
					return defaultValue;
				}
			}

			return proceed.invoke(self, args);
		};

		DelayPipe pipe = null;
		try {
			pipe = (DelayPipe) factory.create(new Class<?>[0], new Object[0], handler);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
			throw new RuntimeException(e);
		}

		return pipe;
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
		pipe.addParameter(NumberParameterBuilder.create("delayTime", 2000L));

		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertEquals(input, result);

		assertEquals(2000L, pipe.getDelayTime());
	}
}
