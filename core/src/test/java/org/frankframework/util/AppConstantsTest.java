package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppConstantsTest {
	private final Logger log = LogUtil.getLogger(this);

	private ClassLoaderMock classLoader;
	private AppConstants constants;
	private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

	@BeforeEach
	public void setUp() {
		classLoader = new ClassLoaderMock(contextClassLoader, false);
		constants = AppConstants.getInstance(classLoader);
	}

	@AfterEach
	public void tearDown() {
		AppConstants.removeInstance(classLoader);
	}

	@Test
	public void onlyInAppConstants() {
		assertEquals("1", constants.getProperty("only.in.appconstants"));
	}

	@Test
	public void onlyInDeploymentSpecifics() {
		assertEquals("2", constants.getProperty("only.in.deploymentspecifics"));
	}

	@Test
	public void overwriteInDeploymentSpecifics() {
		assertEquals("2", constants.getProperty("overwrite.in.deploymentspecifics"));
	}

	@Test
	public void onlyInDeploymentSpecificsParent() {
		assertEquals("3", constants.getProperty("only.in.deploymentspecifics.parent"));
	}

	@Test
	public void overwriteInDeploymentSpecificsParent() {
		assertEquals("3", constants.getProperty("overwrite.in.deploymentspecifics.parent"));
	}

	@Test
	public void onlyInBuildInfo() {
		assertEquals("4", constants.getProperty("only.in.buildinfo"));
	}

	@Test
	public void overwriteInBuildInfo() {
		assertEquals("4", constants.getProperty("overwrite.in.buildinfo"));
	}

	@Test
	public void checkPersistance() {
		AppConstants constants1 = AppConstants.getInstance(classLoader);
		constants1.put("constants1", "1");
		AppConstants constants2 = AppConstants.getInstance(classLoader);
		constants2.put("constants2", "2");
		AppConstants constants3 = AppConstants.getInstance(classLoader);
		constants3.put("constants3", "3");

		assertTrue(constants == constants1, "Singleton instance is not identical");
		assertTrue(constants2 == constants3, "Singleton instance is not identical");

		assertEquals("1", constants.get("constants1"));
		assertEquals("2", constants.get("constants2"));
		assertEquals("3", constants.get("constants3"));
	}

	@Test
	public void removeInstance() {
		AppConstants old = AppConstants.getInstance();
		old.put("dummy-key", "1");
		AppConstants.removeInstance();

		assertFalse(AppConstants.getInstance().contains("dummy-key"), "should not contain key [dummy-key]");
	}

	@Test
	public void removeInstanceWithClassLoader() {
		assertEquals("3", constants.getProperty("only.in.deploymentspecifics.parent"));
		AppConstants.removeInstance(classLoader);

		constants = AppConstants.getInstance(new ClassLoaderMock(contextClassLoader, true));

		assertEquals("changed", constants.getProperty("only.in.deploymentspecifics.parent"));
	}

	@Test
	public void callGetInstanceWithoutClassLoader() {
		assertThrows(IllegalStateException.class, () -> AppConstants.getInstance(null));
	}

	@Test
	public void callRemoveInstanceWithoutClassLoader() {
		assertThrows(IllegalStateException.class, () -> AppConstants.removeInstance(null));
	}

	@Test
	public void callUnresolvedProperty() {
		assertEquals("${resolve.me}", constants.getUnresolvedProperty("unresolved.property"));
	}

	@Test
	public void callResolvedProperty() {
		assertEquals("123", constants.getProperty("unresolved.property"));
	}

	@Test
	public void callNonExistingProperty() {
		assertEquals(null, constants.getProperty("i.dont.exist"));
	}

	@Test
	public void setRuntimePropertyThroughPut() {
		//Remove the default instance
		AppConstants.removeInstance();
		//Create a new one and set a property on the newly created instance
		AppConstants.getInstance().put("dummyConstant", "2.7");
		//Retrieve the property through a different instance
		assertEquals("2.7", constants.get("dummyConstant"));
	}

	@Test
	public void setRuntimePropertyThroughSetProperty() {
		//Remove the default instance
		AppConstants.removeInstance();
		//Create a new one and set a property on the newly created instance
		AppConstants.getInstance().setProperty("dummyConstant2", "2.8");
		//Retrieve the property through a different instance
		assertEquals("2.8", constants.get("dummyConstant2"));
	}

	@Test
	public void setAndGetStringProperty() {
		constants.setProperty("property.type.string", "string");
		assertEquals("string", constants.getString("property.type.string", ""));
	}

	@Test
	public void setAndGetBooleanProperty() {
		constants.setProperty("property.type.boolean", "true");
		assertTrue(constants.getBoolean("property.type.boolean", false));
	}

	@Test
	public void setAndGetIntProperty() {
		constants.setProperty("property.type.int", "123");
		assertEquals(123, constants.getInt("property.type.int", 0));
	}

	@Test
	public void setAndGetLongProperty() {
		constants.setProperty("property.type.long", "123");
		assertEquals(123, constants.getLong("property.type.long", 0));
	}

	@Test
	public void setAndGetDoubleProperty() {
		constants.setProperty("property.type.double", "123.456");
		assertEquals(123.456, constants.getDouble("property.type.double", 0.0), 0);
	}

	@Test
	public void testUtf8EncodedPropertyFile() {
		assertEquals("‘’", constants.getProperty("encoding.utf8"));
	}

	@Test
	public void testSubAppConstants() {
		Properties propertiesStartingWithConfigurations = constants.getAppConstants("configurations");
		assertEquals("true", propertiesStartingWithConfigurations.getProperty("configurations.validate"));
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
			Vector<URL> urls = new Vector<>();
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
