package org.frankframework.jndi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.junit.jupiter.api.Test;
import org.springframework.jndi.JndiTemplate;

public class JndiObjectLocatorTest {

	@Test
	public void jndiObjectLocator() throws NamingException {
		JndiObjectLocator locator = spy(new JndiObjectLocator());
		JndiTemplate template = spy(JndiTemplate.class);
		Context context = mock(Context.class);
		doThrow(NameNotFoundException.class).when(context).lookup(anyString());

		doReturn(context).when(template).getContext();
		doReturn(template).when(locator).getJndiTemplate(any(Properties.class));

		assertNull(locator.lookup("name", null, Object.class));
	}

	@Test
	public void jndiObjectLocatorWithPrefix() throws NamingException {
		JndiObjectLocator locator = spy(new JndiObjectLocator());
		locator.setJndiContextPrefix("java:comp/env/");
		JndiTemplate template = spy(JndiTemplate.class);
		Context context = mock(Context.class);
		doThrow(NameNotFoundException.class).when(context).lookup(anyString());

		doReturn(context).when(template).getContext();
		doReturn(template).when(locator).getJndiTemplate(any(Properties.class));

		assertNull(locator.lookup("name", null, Object.class));
	}

	@Test
	public void doLookup() throws NamingException {
		Object dummyLookupResource = new Object();

		JndiObjectLocator locator = spy(new JndiObjectLocator());
		locator.setJndiContextPrefix("java:comp/env/");
		JndiTemplate template = spy(JndiTemplate.class);
		Context context = mock(Context.class);
		doReturn(dummyLookupResource).when(context).lookup(eq("name"));

		doReturn(context).when(template).getContext();
		doReturn(template).when(locator).getJndiTemplate(any());

		assertEquals(dummyLookupResource, locator.lookup("name", null, Object.class));
	}

	@Test
	public void doLookupWithPrefix() throws NamingException {
		Object dummyLookupResource = new Object();

		JndiObjectLocator locator = spy(new JndiObjectLocator());
		locator.setJndiContextPrefix("java:comp/env/");
		JndiTemplate template = spy(JndiTemplate.class);
		Context context = mock(Context.class);
		doReturn(dummyLookupResource).when(context).lookup(eq("java:comp/env/name"));

		doReturn(context).when(template).getContext();
		doReturn(template).when(locator).getJndiTemplate(any());

		assertEquals(dummyLookupResource, locator.lookup("name", null, Object.class));
	}
}
