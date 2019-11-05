package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AppConstantsTest {
	private Logger log = LogUtil.getLogger(this);

	private ClassLoaderMock classLoader;
	private AppConstants constants;

	@Before
	public void setUp() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		classLoader = new ClassLoaderMock(contextClassLoader);
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


	private class ClassLoaderMock extends ClassLoader {

		public ClassLoaderMock(ClassLoader parent) {
			super(parent);
		}

		@Override
		public URL getResource(String name) {
			// Should never be called by AppConstants!
			throw new IllegalStateException("fail");
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			Vector<URL> urls = new Vector<URL>();

			URL file = getParent().getResource("AppConstants/"+name);
			log.debug("trying to find file ["+name+"] URL["+file+"]");
			if(file == null) {
				throw new IllegalStateException("could not locate resource [AppConstants/"+name+"]");
			}
			urls.add(file);

			if("DeploymentSpecifics.properties".equals(name)) {
				URL parent = getParent().getResource("AppConstants/ParentClassLoader/"+name);
				if(parent == null) {
					throw new IllegalStateException("could not locate resource [AppConstants/ParentClassLoader/"+name+"]");
				}
				urls.add(parent);
			}

			return urls.elements();
		}
	}
}
