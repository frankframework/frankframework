package nl.nn.adapterframework.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.credentialprovider.util.Misc;


public class ServletManagerTest {
	private static ServletManager manager;

	@BeforeClass
	public static void setUp() {
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
		String name = Misc.createNumericUUID();
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
		String name = Misc.createNumericUUID();
		AppConstants.getInstance().setProperty("servlet."+name+".urlMapping", "  test2 , /test3");

		DummyServletImpl servlet = new DummyServletImpl();
		servlet.setUrlMapping(new String[] {"mapping1", "mapping2"});

		DynamicServletRegistration sdr = createAndRegister(name, servlet);

		assertEquals("[/test2, /test3]", sdr.getMappings().toString());
	}



	private DynamicServletRegistration createAndRegister(DummyServletImpl servlet) {
		return createAndRegister(Misc.createNumericUUID(), servlet);
	}

	private DynamicServletRegistration createAndRegister(String name, DummyServletImpl servlet) {
		servlet.setName(name);
		manager.register(servlet);
		return (DynamicServletRegistration) manager.getServletContext().getServletRegistration(name);
	}

	//Mock classes used to test the ServletManagers functionality

	private static class DummyServletImpl extends HttpServlet implements DynamicRegistration.Servlet {
		private @Getter @Setter String name;
		private @Getter String[] urlMappings;
		private @Getter @Setter String[] roles;
		public void setUrlMapping(String urlMapping) {
			setUrlMapping(new String[] {urlMapping} );
		}
		public void setUrlMapping(String[] urlMapping) {
			this.urlMappings = urlMapping;
		}

		@Override
		public HttpServlet getServlet() {
			return this;
		}

		@Override
		public String getUrlMapping() {
			return urlMappings != null ? String.join(",", urlMappings) : null;
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

		public DynamicServletRegistration(String servletName, javax.servlet.Servlet servlet) {
			this.name = servletName;
			servlet = (DummyServletImpl) servlet;
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
