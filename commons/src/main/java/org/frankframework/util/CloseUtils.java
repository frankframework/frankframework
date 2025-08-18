/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collection;

import jakarta.annotation.Nullable;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.input.CloseShieldReader;
import org.apache.commons.io.output.CloseShieldOutputStream;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CloseUtils {

	private CloseUtils() {
		// Don't construct utils class
	}

	/**
	 * Safely close an {@link AutoCloseable}, logging but ignoring any exceptions thrown.
	 *
	 * @param closeable AutoCloseable to close. It is safe to pass {@code null}.
	 */
	public static void closeSilently(@Nullable AutoCloseable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				log.warn("Error closing [{}]", closeable, e);
			}
		}
	}

	/**
	 * Safely close all {@link AutoCloseable}s, logging but ignoring any exceptions thrown.
	 *
	 * @param closeables AutoCloseables to close. It is safe to pass {@code null} values.
	 */
	public static void closeSilently(AutoCloseable... closeables) {
		for (AutoCloseable closeable : closeables) {
			closeSilently(closeable);
		}
	}

	/**
	 * Safely close all {@link AutoCloseable}s, logging but ignoring any exceptions thrown.
	 *
	 * @param closeables AutoCloseables to close. It is safe to pass {@code null}, or for the collection to contain {@code null} values.
	 */
	public static void closeSilently(@Nullable Collection<? extends AutoCloseable> closeables) {
		if (closeables != null) {
			closeables.forEach(CloseUtils::closeSilently);
		}
	}

	public static InputStream dontClose(InputStream stream) {
		return CloseShieldInputStream.wrap(stream);
	}

	public static Reader dontClose(Reader reader) {
		return CloseShieldReader.wrap(reader);
	}

	public static OutputStream dontClose(OutputStream stream) {
		return CloseShieldOutputStream.wrap(stream);
	}
}
