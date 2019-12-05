package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AppConstantsTest {
	private Logger log = LogUtil.getLogger(this);

	private ClassLoaderMock classLoader;
	private AppConstants constants;
	private ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

	@Before
	public void setUp() {
		classLoader = new ClassLoaderMock(contextClassLoader, false);
		constants = AppConstants.getInstance(classLoader);
	}

	@After
	public void tearDown() {
		AppConstants.removeInstance(classLoader);
	}

	@Test
	public void onlyInAppConstants() {
		assertEquals("1", constants.getResolvedProperty("only.in.appconstants"));
	}

	@Test
	public void onlyInDeploymentSpecifics() {
		assertEquals("2", constants.getResolvedProperty("only.in.deploymentspecifics"));
	}

	@Test
	public void overwriteInDeploymentSpecifics() {
		assertEquals("2", constants.getResolvedProperty("overwrite.in.deploymentspecifics"));
	}

	@Test
	public void onlyInDeploymentSpecificsParent() {
		assertEquals("3", constants.getResolvedProperty("only.in.deploymentspecifics.parent"));
	}

	@Test
	public void overwriteInDeploymentSpecificsParent() {
		assertEquals("3", constants.getResolvedProperty("overwrite.in.deploymentspecifics.parent"));
	}

	@Test
	public void onlyInBuildInfo() {
		assertEquals("4", constants.getResolvedProperty("only.in.buildinfo"));
	}

	@Test
	public void overwriteInBuildInfo() {
		assertEquals("4", constants.getResolvedProperty("overwrite.in.buildinfo"));
	}

	@Test
	public void checkPersistance() {
		AppConstants constants1 = AppConstants.getInstance(classLoader);
		constants1.put("constants1", "1");
		AppConstants constants2 = AppConstants.getInstance(classLoader);
		constants2.put("constants2", "2");
		AppConstants constants3 = AppConstants.getInstance(classLoader);
		constants3.put("constants3", "3");

		Assert.assertEquals(constants, constants1);
		Assert.assertEquals(constants2, constants3);

		assertEquals("1", constants.get("constants1"));
		assertEquals("2", constants.get("constants2"));
		assertEquals("3", constants.get("constants3"));
	}

	@Test
	public void removeInstance() {
		AppConstants old = AppConstants.getInstance();
		old.put("dummy-key", "1");
		AppConstants.removeInstance();

		assertFalse("should not contain key [dummy-key]", AppConstants.getInstance().contains("dummy-key"));
	}

	@Test
	public void removeInstanceWithClassLoader() {
		assertEquals("3", constants.getResolvedProperty("only.in.deploymentspecifics.parent"));
		AppConstants.removeInstance(classLoader);

		constants = AppConstants.getInstance(new ClassLoaderMock(contextClassLoader, true));

		assertEquals("changed", constants.getResolvedProperty("only.in.deploymentspecifics.parent"));
	}

	private class ClassLoaderMock extends ClassLoader {
		private boolean simulateReload = false;

		public ClassLoaderMock(ClassLoader parent, boolean simulateReload) {
			super(parent);
			this.simulateReload = simulateReload;
		}

		@Override
		public URL getResource(String name) {
			// Should never be called by AppConstants!
			throw new IllegalStateException("fail");
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			Vector<URL> urls = new Vector<URL>();
			String nameToUse = name;

			URL file = getParent().getResource("AppConstants/"+nameToUse);
			log.debug("trying to find file ["+name+"] URL["+file+"]");
			if(file == null) {
				throw new IllegalStateException("could not locate resource [AppConstants/"+nameToUse+"]");
			}
			urls.add(file);

			if("DeploymentSpecifics.properties".equals(name)) {
				if(simulateReload) {
					nameToUse = "OtherSpecifics.properties";
				}

				URL parent = getParent().getResource("AppConstants/ParentClassLoader/"+nameToUse);
				if(parent == null) {
					throw new IllegalStateException("could not locate resource [AppConstants/ParentClassLoader/"+nameToUse+"]");
				}
				urls.add(parent);
			}

			return urls.elements();
		}
	}
}
