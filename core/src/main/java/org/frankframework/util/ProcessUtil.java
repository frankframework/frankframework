/*
   Copyright 2013 Nationale-Nederlanden, 2023-2025 WeAreFrank!

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;
import org.frankframework.stream.SerializableFileReference;
import org.frankframework.task.TimeoutGuard;

/**
 * Process execution utilities.
 *
 * @author  Gerrit van Brakel
 * @since   4.8
 */
@Log4j2
public class ProcessUtil {

	private static String readStream(InputStream stream) throws IOException {
		StringBuilder result = new StringBuilder();

		BufferedReader bufferedReader = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(stream));
		String line = null;
		// read() instead of readLine() results in JVM core dumps (this
		// also happens when using InputStream or BufferedInputStream)
		// using WebSphere Studio Application Developer (Windows)
		// Version: 5.1.2, Build id: 20040506_1735
		while ((line = bufferedReader.readLine()) != null) {
			result.append(line).append("\n");
		}
		return result.toString();
	}

	protected static String getCommandLine(List<String> command) {
		if (command==null || command.isEmpty()) {
			return "";
		}
		StringBuilder result = new StringBuilder(command.getFirst());
		for (int i = 1; i < command.size(); i++) {
			result.append(" ").append(command.get(i));
		}
		return result.toString();
	}

	public static List<String> splitUpCommandString(String command) {
		return Arrays.asList(command.split("(\\s|\f)+"));
	}

	public static Message executeCommand(String command) throws IOException {
		try {
			return executeCommand(splitUpCommandString(command),0);
		} catch (TimeoutException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Execute a command as a process in the operating system.
	 * Timeout is passed in seconds, or 0 to wait indefinitely until the process ends
	 */
	public static Message executeCommand(List<String> command, int timeout) throws IOException, TimeoutException {
		Path tempFile = executeCommandInternal(command, timeout);

		// Assume that if the above method returned successfully we are now in charge of cleaning the file.
		return PathMessage.asTemporaryMessage(tempFile);
	}

	/**
	 * Complex method that deals with the execution of the command.
	 * The results are stored in a file to allow for large results.
	 * If something goes wrong the file needs to be cleaned.
	 * See the CleanerProvider and CleanerAction below.
	 */
	@SuppressWarnings("java:S2142") // Don't re-interrupt
	private static Path executeCommandInternal(List<String> command, int timeout) throws IOException, TimeoutException {
		Path tempDir = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		Path stdoutFile = Files.createTempFile(tempDir, "msg", "dat");

		final Process process;
		try {
			process = new ProcessBuilder(command.toArray(new String[0]))
					.redirectOutput(Redirect.to(stdoutFile.toFile()))
					.start();
		} catch (Throwable t) {
			Files.delete(stdoutFile);
			throw new IOException("unable to execute command [" + getCommandLine(command) + "]", t);
		}

		TimeoutGuard tg = new TimeoutGuard("ProcessUtil", process::destroy);
		tg.activateGuard(timeout);

		try {
			// Wait until the process is completely finished, or timeout is expired.
			process.waitFor();

			// Process execution has been completed.
			// Read the errors of the process.
			String errors = readStream(process.getErrorStream());
			if (StringUtils.isNotEmpty(errors)) {
				log.warn("command [{}] had error output [{}]", getCommandLine(command), errors);
			}

			// Throw an exception if the command returns an error exit value.
			int exitValue = process.exitValue();
			if (exitValue != 0) {
				String outputStr = Files.readString(stdoutFile);
				throw new IOException("nonzero exit value [" + exitValue + "] output was [" + outputStr + "], error output was [" + errors + "]");
			}

			// Everything went well?
			return stdoutFile;

			// Yes catch the above Exception, so we can uniform the error handling (file-cleanup).
		} catch(Exception e) {
			CleanupFileAction cleanupFileAction = new CleanupFileAction(stdoutFile);
			Cleanable cleanable = CleanerProvider.register(process, cleanupFileAction);

			try {
				Files.delete(stdoutFile);
				// If we've reached this point we were able to remove the file, no need for the cleaner anymore :).
				CleanerProvider.clean(cleanable);
			} catch(IOException ignored) {
				// We were not able to directly clean the File, rely on the CleanerProvider to clean our mess.
			}

			if (tg.threadKilled()) {
				throw new TimeoutException("command ["+getCommandLine(command)+"] timed out", e);
			} else {
				throw new IOException("error while executing command ["+getCommandLine(command)+"]", e);
			}
		} finally {
			tg.cancel();
		}
	}

	private static class CleanupFileAction implements Runnable {
		private final Path fileToClean;

		private CleanupFileAction(Path fileToClean) {
			this.fileToClean = fileToClean;
		}

		@Override
		public void run() {
			try {
				Files.deleteIfExists(fileToClean);
			} catch (Exception e) {
				log.warn("failed to remove file reference [{}]. Exception message: {}", fileToClean, e.getMessage());
			}
		}
	}
}
