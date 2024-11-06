package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

import org.frankframework.core.IWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.NumberParameter;
import org.frankframework.parameters.ParameterList;

import org.frankframework.parameters.ParameterValue;
import org.frankframework.stream.Message;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

import java.lang.reflect.InvocationTargetException;

@Tag("mytag")
public class DelayPipeTest extends PipeTestBase<DelayPipe> {

	@Override
	public DelayPipe createPipe() {
		ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(DelayPipe.class);

		var ref = new Object() {
			Message message;
			PipeLineSession session;
		};

		MethodHandler handler = (self, method, proceed, args) -> {
			ValueFromParameter valueFromParameter = method.getAnnotation(ValueFromParameter.class);

			if (valueFromParameter != null && ref.session != null) {
				String methodName = method.getName().replaceFirst("^get", "");
				methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);

				final Object defaultValue = proceed.invoke(self, args);

				IWithParameters selfWithParameters = (IWithParameters) self;
				ParameterList parameterList = selfWithParameters.getParameterList();

				if (parameterList == null) {
					return defaultValue;
				}

				try {
					if (!parameterList.hasParameter(methodName)) {
						return defaultValue;
					}

					final ParameterValue value = parameterList.getValue(null, parameterList.findParameter(methodName), ref.message, ref.session, true);

					return value.asLongValue((Long) defaultValue);
				} catch (ParameterException e) {
					return defaultValue;
				}
			}

			if (method.getName().equals("doPipe")) {
				ref.message = (Message) args[0];
				ref.session = (PipeLineSession) args[1];

				final Object result = proceed.invoke(self, args);

				ref.message = null;
				ref.session = null;

				return result;
			}

			return proceed.invoke(self, args);
		};

		DelayPipe pipe;
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

		assertEquals(1000L, pipe.getDelayTime());
	}
}
