package org.frankframework.larva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.context.ApplicationContext;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.IScopeProvider;

// This class is also tested in the RunLarvaTests junit-tester and larva scenario's.
public class XsltProviderListenerTest {

	@ParameterizedTest
	@NullAndEmptySource
	public void filenameIsMandatory(String nullAndEmpty) throws Exception {
		try (XsltProviderListener listener = new XsltProviderListener()) {
			listener.setFilename(nullAndEmpty);
			assertThrows(ConfigurationException.class, listener::configure);
		}
	}

	@Test
	public void testConfigurationScope() throws Exception {
		IbisManager mockIbisManager = spy();
		Configuration mockConfiguration = mock();
		when(mockConfiguration.getName()).thenReturn("testConfigName");
		mockIbisManager.addConfiguration(mockConfiguration);

		try (XsltProviderListener listener = new XsltProviderListener()) {
			ApplicationContext applicationContext = mock(ApplicationContext.class);
			when(applicationContext.containsBean("ibisManager")).thenReturn(true);
			when(applicationContext.getBean("ibisManager", IbisManager.class)).thenReturn(mockIbisManager);
			listener.setApplicationContext(applicationContext);
			listener.setConfigurationName("testConfigName");

			IScopeProvider provider = listener.findScope();
			assertNotNull(provider);
			assertEquals(mockConfiguration, provider);
		}
	}

	@Test
	public void dontErrorWhenNoIbisManager() throws Exception {
		IbisManager mockIbisManager = spy();
		Configuration mockConfiguration = mock();
		when(mockConfiguration.getName()).thenReturn("testConfigName");
		mockIbisManager.addConfiguration(mockConfiguration);

		try (XsltProviderListener listener = new XsltProviderListener()) {
			ApplicationContext applicationContext = mock(ApplicationContext.class);
			when(applicationContext.containsBean("ibisManager")).thenReturn(false);
			listener.setApplicationContext(applicationContext);
			listener.setConfigurationName("testConfigName");

			assertThrows(ConfigurationException.class, listener::findScope);
		}
	}
}
