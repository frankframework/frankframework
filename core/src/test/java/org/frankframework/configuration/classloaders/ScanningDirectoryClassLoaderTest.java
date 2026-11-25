/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.configuration.classloaders;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.IbisManager;
import org.frankframework.management.Action;
import org.frankframework.testutil.junit.WithTimeTravel;
import org.frankframework.util.TimeProvider;

public class ScanningDirectoryClassLoaderTest extends ConfigurationClassLoaderTestBase<ScanningDirectoryClassLoader> {

	private ScanningDirectoryClassLoader scanningClassLoader;
	private IbisManager ibisManager;

	@Override
	public ScanningDirectoryClassLoader createClassLoader(ClassLoader parent) throws Exception {
		URL file = this.getClass().getResource("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(file);

		ibisManager = mock(IbisManager.class);

		scanningClassLoader = new ScanningDirectoryClassLoader(parent);
		scanningClassLoader.setDirectory(file.getFile());
		scanningClassLoader.setIbisManager(ibisManager);
		appConstants.setProperty("configurations." + getConfigurationName() + ".directory", file.getFile());
		return scanningClassLoader;
	}

	@AfterEach
	public void shutdownScanner() {
		scanningClassLoader.destroy();
	}

	/**
	 * A changed file makes the ClassLoader reload its configuration through the IbisManager, which is the
	 * second caller of {@link IbisManager#handleAction} besides the console and the management bus.
	 */
	@Test
	@WithTimeTravel
	public void scanReloadsTheConfigurationWhenAFileHasChanged(TimeProvider.TimeTraveller timeTraveller) throws Exception {
		// Arrange: start the scanner in 1970, so every file in the directory has been modified since its last scan
		shutdownScanner();
		timeTraveller.setTime(0L);
		createAndConfigure(".");

		// Act
		scanningClassLoader.scan();

		// Assert
		verify(ibisManager).handleAction(eq(Action.RELOAD), eq(getConfigurationName()), isNull(), isNull(), any(String.class), eq(false));
	}

	/** The scanner started just now, so none of the files can have been modified since. */
	@Test
	public void scanLeavesTheConfigurationAloneWhenNothingHasChanged() {
		// Act
		scanningClassLoader.scan();

		// Assert
		verify(ibisManager, never()).handleAction(any(), any(), any(), any(), any(), anyBoolean());
	}
}
