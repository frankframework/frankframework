package org.frankframework.jndi;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.ServiceUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.jndi.JndiTemplate;

public class TestJmsConnectionFactoryFactory {

	@Test
	public void testServiceUnavailableExceptionWhenDoingLookup() throws NamingException {
		JmsConnectionFactoryFactory factory = new JmsConnectionFactoryFactory();

		JndiObjectLocator locator = spy(new JndiObjectLocator());
		JndiTemplate template = spy(JndiTemplate.class);
		Context context = mock(Context.class);
		doThrow(ServiceUnavailableException.class).when(context).lookup(anyString());

		doReturn(context).when(template).getContext();
		doReturn(template).when(locator).getJndiTemplate(any(Properties.class));
		factory.setObjectLocators(List.of(locator));

		IllegalStateException e = assertThrows(IllegalStateException.class, () -> factory.getConnectionFactory("dummyName"));
		assertInstanceOf(NoInitialContextException.class, e.getCause());
		assertTrue(factory.getObjectInfo().isEmpty());
	}
}
