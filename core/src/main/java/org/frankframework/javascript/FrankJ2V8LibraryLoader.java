/*
   Copyright 2019-2024 WeAreFrank!

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
/*******************************************************************************
 * File copied from the original j2v8 LibraryLoader.java source, version 4.6.0.
 * It has been modified to avoid having to set the <<ALL FILES>> execute permission
 * for loading the native library. Only the call to 'chmod()' is disabled.
 *
 * Original Copyright statement:
 *
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	EclipseSource - initial API and implementation
 *
 ******************************************************************************/
package org.frankframework.javascript;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class FrankJ2V8LibraryLoader {

	private static final String SEPARATOR = System.getProperty("file.separator");
	private static final String DELIMITER = System.getProperty("line.separator");
	private static final String OS_NAME = System.getProperty("os.name") + System.getProperty("java.specification.vendor");

	static final String SWT_LIB_DIR = ".j2v8";

	private static String computeLibraryShortName() {
		String base = "j2v8";
		String osSuffix = getOS();
		String archSuffix = getArchSuffix();
		return base + "_" + osSuffix + "_" + archSuffix;
	}

	private static String computeLibraryFullName() {
		return "lib" + computeLibraryShortName() + "." + getOSFileExtension();
	}

	public static void loadLibrary(final String tempDirectory) {
		StringBuilder message = new StringBuilder();
		String libShortName = computeLibraryShortName();
		String libFullName = computeLibraryFullName();
		String ideLocation = System.getProperty("user.dir") + SEPARATOR + "jni" + SEPARATOR + computeLibraryFullName();
		String path = null;

		/* Try loading library from java library path */
		if (load(libShortName, message)) {
			return;
		}

		/* Try loading library from the IDE location */
		if (new File(ideLocation).exists()) {
			if (load(ideLocation, message)) {
				return;
			}
		}

		if (tempDirectory != null) {
			path = tempDirectory;
		} else {
			path = System.getProperty("user.home"); //$NON-NLS-1$
		}

		if (extract(path + SEPARATOR + libFullName, libFullName, message)) {
			return;
		}

		/* Failed to find the library */
		throw new UnsatisfiedLinkError("Could not load J2V8 library. Reasons: " + message); //$NON-NLS-1$
	}

	private static boolean load(final String libName, final StringBuilder message) {
		try {
			if (libName.indexOf(SEPARATOR) != -1) {
				System.load(libName);
			} else {
				System.loadLibrary(libName);
			}
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (message.length() == 0) {
				message.append(DELIMITER);
			}
			message.append('\t');
			message.append(e.getMessage());
			message.append(DELIMITER);
		}
		return false;
	}

	private static boolean extract(final String fileName, final String mappedName, final StringBuilder message) {
		FileOutputStream os = null;
		InputStream is = null;
		File file = new File(fileName);
		boolean extracted = false;
		try {
			if (file.exists()) {
				file.delete();
			}
			is = FrankJ2V8LibraryLoader.class.getResourceAsStream("/" + mappedName); //$NON-NLS-1$
			if (is != null) {
				extracted = true;
				int read;
				byte[] buffer = new byte[4096];
				os = new FileOutputStream(fileName);
				while ((read = is.read(buffer)) != -1) {
					os.write(buffer, 0, read);
				}
				os.close();
				is.close();
				// removed call to chmod(), as it appears to be unnecessary, and causes <<ALL FILES>> execute permission exceptions
				if (load(fileName, message)) {
					return true;
				}
			}
		} catch (Throwable e) {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e1) {
			}
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e1) {
			}
			if (extracted && file.exists()) {
				file.delete();
			}
		}
		return false;
	}

	private static boolean isWindows() {
		return OS_NAME.startsWith("Windows");
	}

	private static boolean isMac() {
		return OS_NAME.startsWith("Mac");
	}

	private static boolean isLinux() {
		return OS_NAME.startsWith("Linux");
	}

	private static boolean isNativeClient() {
		return OS_NAME.startsWith("nacl");
	}

	private static String getArchSuffix() {
		String arch = System.getProperty("os.arch");
		if ("i686".equals(arch)) {
			return "x86";
		} else if ("amd64".equals(arch)) {
			return "x86_64";
		} else if ("nacl".equals(arch)) {
			return "armv7l";
		} else if ("aarch64".equals(arch)) {
			return "armv7l";
		}
		return arch;
	}

	private static String getOSFileExtension() {
		if (isWindows()) {
			return "dll";
		} else if (isMac()) {
			return "dylib";
		} else if (isLinux()) {
			return "so";
		} else if (isNativeClient()) {
			return "so";
		}
		throw new UnsatisfiedLinkError("Unsupported platform: " + OS_NAME);
	}

	private static String getOS() {
		if (isWindows()) {
			return "win32";
		} else if (isMac()) {
			return "macosx";
		} else if (isLinux()) {
			return "linux";
		}
		throw new UnsatisfiedLinkError("Unsupported platform: " + OS_NAME);
	}

}
