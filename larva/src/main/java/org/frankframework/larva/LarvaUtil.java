package org.frankframework.larva;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.output.LarvaWriter;

@Log4j2
public class LarvaUtil {

	private LarvaUtil() {
		// Private constructor to prevent creating instances
	}

	public static String getAbsolutePath(String parent, String child) {
		return getAbsolutePath(parent, child, false);
	}

	/**
	 * Returns the absolute pathname for the child pathname. The parent pathname
	 * is used as a prefix when the child pathname is an not absolute.
	 *
	 * @param parent  the parent pathname to use
	 * @param child   the child pathname to convert to a absolute pathname
	 */
	public static String getAbsolutePath(String parent, String child,
										 boolean addFileSeparator) {
		File result;
		File file = new File(child);
		if (file.isAbsolute()) {
			result = file;
		} else {
			result = new File(parent, child);
		}
		String absPath = FilenameUtils.normalize(result.getAbsolutePath());
		if (addFileSeparator) {
			return absPath + File.separator;
		} else {
			return absPath;
		}
	}

	public static String formatDuration(long durationInMs) {
		Duration duration = Duration.ofMillis(durationInMs);
		if (duration.toMinutesPart() == 0 && duration.toSecondsPart() == 0) {
			// Only milliseconds (e.g. 123ms)
			return duration.toMillisPart() + "ms";
		} else if (duration.toMinutesPart() == 0) {
			// Seconds and milliseconds (e.g. 1s 123ms)
			return duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		} else {
			// Minutes, seconds and milliseconds (e.g. 1m 1s 123ms)
			return duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		}
	}

	public static @Nullable String getParentOfWebappRoot() {
		URL rootResource = LarvaUtil.class.getResource("/");
		if (rootResource == null) {
			return null;
		}
		String realPath = rootResource.getPath();
		return new File(realPath).getParent();
	}

	protected static @Nonnull Properties readProperties(LarvaWriter out, File propertyFile) {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(propertyFile));
		} catch (IOException e) {
			out.errorMessage("Cannot read property file: " + propertyFile.getAbsolutePath(), e);
		}
		return properties;
	}
}
