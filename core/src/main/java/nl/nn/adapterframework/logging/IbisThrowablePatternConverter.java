package nl.nn.adapterframework.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.CodeSource;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;
import org.apache.logging.log4j.util.Strings;

@Plugin(name = "IbisPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "iEx", "iThrowable" })
public final class IbisThrowablePatternConverter extends ThrowablePatternConverter {

	private final String CAUSED_BY = "Caused by: ";

	private IbisThrowablePatternConverter(final Configuration config, final String[] options) {
		super("IbisThrowablePatternConverter", "throwable", options, config);
	}

	public static IbisThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
		return new IbisThrowablePatternConverter(config, options);
	}

	@Override
	public void format(final LogEvent event, final StringBuilder buffer) {
		final Throwable throwable = event.getThrown();
		if (throwable != null) {
			final int len = buffer.length();
			if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
				buffer.append(' ');
			}
			throwablePrinter(null, throwable, buffer);
		}
	}

	/** Recursively prints the trace */
	private void throwablePrinter(Throwable parent, Throwable throwable, final StringBuilder buffer) {
		final StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));

		StackTraceElement[] elements = throwable.getStackTrace();
		final String[] linesToBePrinted = writer.toString().split(Strings.LINE_SEPARATOR);

		int stackIndex = elements.length - 1;
		int enclosingStackIndex = 0;
		if (parent != null) {
			StackTraceElement[] wrapping = parent.getStackTrace();
			enclosingStackIndex = parent.getStackTrace().length - 1;
			while (stackIndex >= 0 && enclosingStackIndex >= 0
					&& elements[stackIndex].equals(wrapping[enclosingStackIndex])) {
				stackIndex--;
				enclosingStackIndex--;
			}
		}

		Class<?> clazz = null;
		for (int i = 0; i <= stackIndex + 1; ++i) {
			buffer.append(linesToBePrinted[i]);
			if (i != 0 && i <= elements.length) {
				try {
					clazz = null;
					clazz = Class.forName(elements[i - 1].getClassName());
				} catch (ClassNotFoundException e) {
				}
				buffer.append(getPackageInfo(clazz));
			}
			buffer.append(Strings.LINE_SEPARATOR);
		}
		if (parent != null) {
			int numberOfCommonLines = elements.length - 1 - stackIndex;
			// append common lines
			buffer.append("\t ... " + (numberOfCommonLines) + " more" + Strings.LINE_SEPARATOR);
		}
		if (throwable.getCause() != null) {
			buffer.append(CAUSED_BY);
			throwablePrinter(throwable, throwable.getCause(), buffer);
		}
	}

	private String getPackageInfo(final Class<?> callerClass) {
		String location = "?";
		String version = "?";
		if (callerClass != null) {
			try {
				final CodeSource source = callerClass.getProtectionDomain().getCodeSource();
				if (source != null) {
					final URL locationURL = source.getLocation();
					if (locationURL != null) {
						final String str = locationURL.toString().replace('\\', '/');
						int index = str.lastIndexOf("/");
						if (index >= 0 && index == str.length() - 1) {
							index = str.lastIndexOf("/", index - 1);
						}
						location = str.substring(index + 1);
					}
				}
			} catch (final Exception ex) {
				// Ignore the exception.
			}
			final Package pkg = callerClass.getPackage();
			if (pkg != null) {
				final String ver = pkg.getImplementationVersion();
				if (ver != null) {
					version = ver;
				}
			}
		}
		return " [" + location + ":" + version + "]";
	}

}
