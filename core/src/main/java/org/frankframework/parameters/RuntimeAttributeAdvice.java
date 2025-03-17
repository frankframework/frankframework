/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.parameters;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.frankframework.core.IWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;

import org.frankframework.stream.Message;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class RuntimeAttributeAdvice {

	record DoPipeInformation(Message message, PipeLineSession session) { }

	private final Map<Object, DoPipeInformation> executions = new HashMap<>();

	@Around("@annotation(valueFromParameter)")
	public Object handleGetter(ProceedingJoinPoint pjp, ValueFromParameter valueFromParameter) throws Throwable {
		String methodName = pjp.getSignature().getName().replaceFirst("^get", "");
		methodName = Character.toLowerCase(methodName.charAt(0)) + methodName.substring(1);

		final Object defaultValue = pjp.proceed();

		IWithParameters selfWithParameters = (IWithParameters) pjp.getThis();
		ParameterList parameterList = selfWithParameters.getParameterList();

		if (parameterList == null) {
			return defaultValue;
		}

		try {
			if (!parameterList.hasParameter(methodName)) {
				return defaultValue;
			}

			DoPipeInformation doPipeInformation = executions.get(pjp.getThis());
			if (doPipeInformation == null) {
				return defaultValue;
			}

			Message message = doPipeInformation.message;
			PipeLineSession session = doPipeInformation.session;

			if (session == null) {
				return defaultValue;
			}

			final ParameterValue value = parameterList.getValue(null, parameterList.findParameter(methodName), message, session, true);

			return value.asLongValue((Long) defaultValue);
		} catch (ParameterException e) {
			return defaultValue;
		}
	}

	@Around(value = "execution(* doPipe(..))") //  && args(message) && args(session)", argNames = "pjp,message,session
	public Object handleDoPipe(ProceedingJoinPoint pjp) throws Throwable { //, Message message, PipeLineSession session
		Message message = (Message) pjp.getArgs()[0];
		PipeLineSession session = (PipeLineSession) pjp.getArgs()[1];

		this.executions.put(pjp.getThis(), new DoPipeInformation(message, session));
		final Object result = pjp.proceed();
		this.executions.remove(pjp.getThis());

		return result;
	}

}
