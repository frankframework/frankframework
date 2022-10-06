/*
   Copyright 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.testutil.mock;

import static org.mockito.Mockito.spy;

import org.mockito.Mockito;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;

public class MockIbisManager extends DefaultIbisManager implements ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;

	public MockIbisManager() {
		IbisContext ibisContext = spy(new IbisContext());

		//Delegate 'getBean' request to the ApplicationContext
		Mockito.doAnswer(invocation -> {
			String name = (String) invocation.getArguments()[0];
			Class<?> type = (Class<?>) invocation.getArguments()[1];
			return applicationContext.getBean(name, type);
		}).when(ibisContext).getBean(Mockito.anyString(), (Class<?>) Mockito.any(Class.class));

		//Delegate 'createBeanAutowireByName' requests to the ApplicationContext
		Mockito.doAnswer(invocation -> {
			Class<?> type = (Class<?>) invocation.getArguments()[0];
			return createBean(type);
		}).when(ibisContext).createBeanAutowireByName((Class<?>) Mockito.any(Class.class));

		setIbisContext(ibisContext);
	}

	@SuppressWarnings("unchecked")
	private <T> T createBean(Class<T> type) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(type, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	@Override
	public void shutdown() {
		unload((String) null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Don't initialize a TransactionManager
	}
}
