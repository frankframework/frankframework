package org.frankframework.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;
import jakarta.servlet.http.HttpServlet;

import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.lifecycle.servlets.IAuthenticator;
import org.frankframework.lifecycle.servlets.SecuritySettings;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.util.UUIDUtil;


public class ServletManagerTest {
	private static ServletManager manager;
	private MockEnvironment properties;

	@BeforeAll
	public static void prepare() throws Exception {
		ServletContext context = new MockServletContext() {
			private Map<String, Dynamic> dynamic = new HashMap<>();
			@Override
			public Dynamic addServlet(String servletName, jakarta.servlet.Servlet servlet) {
				return dynamic.compute(servletName, (k,v) -> new DynamicServletRegistration(servletName, servlet));
			}
			@Override
			public ServletRegistration getServletRegistration(String servletName) {
				return dynamic.get(servletName);
			}
		};
		manager = new ServletManager();
		manager.setServletContext(context);

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		MockEnvironment environment = new MockEnvironment();
		doReturn(environment).when(applicationContext).getEnvironment();
		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		doReturn(beanFactory).when(applicationContext).getAutowireCapableBeanFactory();
		doReturn(new DummyAuthenticator()).when(beanFactory).createBean(isA(IAuthenticator.class.getClass()), eq(AutowireCapableBeanFactory.AUTOWIRE_BY_NAME), eq(false));
		manager.setApplicationContext(applicationContext);

		manager.afterPropertiesSet();
	}

	private static class DummyAuthenticator implements IAuthenticator {

		@Override
		public void registerServlet(ServletConfiguration config) {
			// NOOP
		}

		@Override
		public void build() {
			// NOOP
		}

		@Override
		public SecurityFilterChain configureHttpSecurity(HttpSecurity http) {
			// NOOP
			return null;
		}
	}

	@BeforeEach
	public void setUp() {
		properties = new MockEnvironment();
		properties.setProperty("dtap.stage", "ACC");
		properties.setProperty(SecuritySettings.HTTPS_ENABLED_KEY, "confidential");
		properties.setProperty(SecuritySettings.AUTH_ENABLED_KEY, "false");

		SecuritySettings.setupDefaultSecuritySettings(properties);
	}

	@Test
	public void testServletWithSpaceInName() {
		IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> {
			createAndRegister("name with space", new DummyServletImpl());
		});
		assertThat(iae.getMessage(), CoreMatchers.containsString("servlet name may not contain spaces"));
	}

	@Test
	public void testUrlMapping() {
		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping("test");

		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals("[/test]", sdr.getMappings().toString());
	}

	@Test
	public void testUrlMappingOverride() {
		String name = UUIDUtil.createNumericUUID();
		properties.setProperty("servlet."+name+".urlMapping", " test2 ");

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping("not-used");

		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/test2]", sdr.getMappings().toString());
	}

	@Test
	public void testUrlMultipleMappings() {
		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping(new String[] {"mapping1", "/mapping2"});

		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals("[/mapping1, /mapping2]", sdr.getMappings().toString());
	}

	@Test
	public void testUrlMultipleMappingsOverride() {
		String name = UUIDUtil.createNumericUUID();
		properties.setProperty("servlet."+name+".urlMapping", "  test2 , /test3"); //contains spaces ;)

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping(new String[] {"mapping1", "mapping2"});

		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/test2, /test3]", sdr.getMappings().toString());
	}

	@Test
	public void testDefaultTransportGuarantee() {
		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping("/test4");

		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals("[/test4]", sdr.getMappings().toString());
		assertEquals(TransportGuarantee.CONFIDENTIAL, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeOverride() {
		String name = UUIDUtil.createNumericUUID();
		properties.setProperty("servlet."+name+".transportGuarantee", "none");

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping("/test5");

		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/test5]", sdr.getMappings().toString());
		assertEquals(TransportGuarantee.NONE, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobal() {
		MockEnvironment properties = new MockEnvironment();
		properties.setProperty(SecuritySettings.HTTPS_ENABLED_KEY, "confidential");
		SecuritySettings.setupDefaultSecuritySettings(properties);

		DummyServletImpl servlet = new DummyServletImpl();
		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals(TransportGuarantee.CONFIDENTIAL, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobalLOC() {
		MockEnvironment properties = new MockEnvironment();
		properties.setProperty("dtap.stage", "LOC");
		properties.setProperty(SecuritySettings.HTTPS_ENABLED_KEY, "confidential");
		SecuritySettings.setupDefaultSecuritySettings(properties);

		DummyServletImpl servlet = new DummyServletImpl();
		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals(TransportGuarantee.CONFIDENTIAL, sdr.getServletSecurity().getTransportGuarantee());
	}



	private DynamicServletRegistration createAndRegister(DummyServletImpl servlet) {
		return createAndRegister(UUIDUtil.createNumericUUID(), servlet);
	}

	private DynamicServletRegistration createAndRegister(String name, DummyServletImpl servlet) {
		servlet.setName(name);
		ServletConfiguration config = new ServletConfiguration();
		config.setEnvironment(properties);
		config.afterPropertiesSet();
		config.fromServlet(servlet);
		manager.register(config);
		return (DynamicServletRegistration) manager.getServletContext().getServletRegistration(name);
	}

	// Mock classes used to test the ServletManager functionality

	private static class DummyServletImpl extends HttpServlet implements DynamicRegistration.Servlet {
		private @Getter @Setter String name;
		private @Getter String[] urlMappings = new String[] {"dummy-path"};
		private @Getter @Setter String[] accessGrantingRoles;
		public void setUrlMapping(String urlMapping) {
			setUrlMapping(new String[] {urlMapping} );
		}
		public void setUrlMapping(String[] urlMapping) {
			this.urlMappings = urlMapping;
		}

		@Override
		public String getUrlMapping() {
			return String.join(",", urlMappings);
		}
	}

	private static class DynamicServletRegistration implements ServletRegistration.Dynamic {
		private @Getter @Setter int loadOnStartup = 0;
		private @Getter @Setter boolean asyncSupported;
		private @Getter String name;
		private @Getter Map<String, String> initParameters = new HashMap<>();
		private @Getter Set<String> mappings = new TreeSet<>();
		private @Getter String runAsRole = null;
		private @Getter DummyServletImpl servlet;
		private @Getter ServletSecurityElement servletSecurity;

		public DynamicServletRegistration(String servletName, jakarta.servlet.Servlet servlet) {
			this.name = servletName;
			this.servlet = (DummyServletImpl) servlet;
		}

		@Override
		public Set<String> addMapping(String... urlPatterns) {
			mappings.addAll(Arrays.asList(urlPatterns));
			return null;
		}

		@Override
		public String getClassName() {
			return this.getClass().getCanonicalName();
		}

		@Override
		public Set<String> setInitParameters(Map<String, String> initParameters) {
			this.initParameters.putAll(initParameters);
			return null;
		}

		@Override
		public boolean setInitParameter(String name, String value) {
			initParameters.put(name, value);
			return true;
		}

		@Override
		public String getInitParameter(String name) {
			return initParameters.get(name);
		}

		@Override
		public Set<String> setServletSecurity(ServletSecurityElement constraint) {
			this.servletSecurity = constraint;
			return null;
		}

		@Override
		public void setMultipartConfig(MultipartConfigElement multipartConfig) {
			throw new NotImplementedException();
		}

		@Override
		public void setRunAsRole(String roleName) {
			throw new NotImplementedException();
		}
	}
}
