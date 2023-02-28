package nl.nn.adapterframework.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.http.HttpServlet;

import nl.nn.adapterframework.util.UUIDUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockServletContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.lifecycle.servlets.IAuthenticator;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.util.AppConstants;


public class ServletManagerTest {
	private static ServletManager manager;

	@BeforeAll
	public static void prepare() throws Exception {
		ServletContext context = new MockServletContext() {
			private Map<String, Dynamic> dynamic = new HashMap<>();
			@Override
			public Dynamic addServlet(String servletName, javax.servlet.Servlet servlet) {
				return dynamic.compute(servletName, (k,v) -> new DynamicServletRegistration(servletName, servlet));
			}
			@Override
			public ServletRegistration getServletRegistration(String servletName) {
				return dynamic.get(servletName);
			}
		};
		manager = new ServletManager(context);

		ApplicationContext applicationContext = mock(ApplicationContext.class);
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
	}

	@BeforeEach
	public void setUp() {
		Properties properties = new Properties();
		properties.setProperty("dtap.stage", "ACC");
		properties.setProperty(ServletManager.HTTPS_ENABLED_KEY, "confidential");
		properties.setProperty(ServletManager.AUTH_ENABLED_KEY, "false");

		ServletManager.setupDefaultSecuritySettings(properties);
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
		AppConstants.getInstance().setProperty("servlet."+name+".urlMapping", " test2 ");

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
		AppConstants.getInstance().setProperty("servlet."+name+".urlMapping", "  test2 , /test3"); //contains spaces ;)

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
		AppConstants.getInstance().setProperty("servlet."+name+".transportGuarantee", "none");

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping("/test5");

		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/test5]", sdr.getMappings().toString());
		assertEquals(TransportGuarantee.NONE, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobal1() {
		Properties properties = new Properties();
		properties.setProperty(ServletManager.HTTPS_ENABLED_KEY, "none");
		ServletManager.setupDefaultSecuritySettings(properties);

		String name = UUIDUtil.createNumericUUID();

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping(name);
		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/"+name+"]", sdr.getMappings().toString());
		assertEquals(TransportGuarantee.NONE, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobal2() {
		Properties properties = new Properties();
		properties.setProperty(ServletManager.HTTPS_ENABLED_KEY, "confidential");
		ServletManager.setupDefaultSecuritySettings(properties);

		DummyServletImpl servlet = new DummyServletImpl();
		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals(TransportGuarantee.CONFIDENTIAL, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobalLOC1() {
		Properties properties = new Properties();
		properties.setProperty("dtap.stage", "LOC");
		ServletManager.setupDefaultSecuritySettings(properties);

		DummyServletImpl servlet = new DummyServletImpl();
		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals(TransportGuarantee.NONE, sdr.getServletSecurity().getTransportGuarantee());
	}

	@Test
	public void testTransportGuaranteeGlobalLOC2() {
		Properties properties = new Properties();
		properties.setProperty("dtap.stage", "LOC");
		properties.setProperty(ServletManager.HTTPS_ENABLED_KEY, "confidential");
		ServletManager.setupDefaultSecuritySettings(properties);

		DummyServletImpl servlet = new DummyServletImpl();
		DynamicServletRegistration sdr = createAndRegister(servlet);

		assertEquals(TransportGuarantee.CONFIDENTIAL, sdr.getServletSecurity().getTransportGuarantee());
	}



	private DynamicServletRegistration createAndRegister(DummyServletImpl servlet) {
		return createAndRegister(UUIDUtil.createNumericUUID(), servlet);
	}

	private DynamicServletRegistration createAndRegister(String name, DummyServletImpl servlet) {
		servlet.setName(name);
		manager.register(servlet);
		return (DynamicServletRegistration) manager.getServletContext().getServletRegistration(name);
	}

	//Mock classes used to test the ServletManagers functionality

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

		public DynamicServletRegistration(String servletName, javax.servlet.Servlet servlet) {
			this.name = servletName;
			this.servlet = (DummyServletImpl) servlet;
		}

		@Override
		public Set<String> addMapping(String... urlPatterns) {
			for(String pattern : urlPatterns) {
				mappings.add(pattern);
			}
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
