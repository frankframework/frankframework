package nl.nn.adapterframework.extensions.log4j;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.PatternSelector;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

import java.nio.charset.Charset;

/**
 * One implementation of {@link IbisMaskingLayout},
 * which uses {@link PatternLayout} as abstract string layout
 * to serialize the given log events.
 */
@Plugin(name = "IbisPatternLayout", category = "Core", elementType = "layout", printObject = true)
public class IbisPatternLayout extends IbisMaskingLayout {

	IbisPatternLayout(final Configuration config, final RegexReplacement replace, final String eventPattern,
					  final PatternSelector patternSelector, final Charset charset, final boolean alwaysWriteExceptions,
					  final boolean disableAnsi, final boolean noConsoleNoAnsi, final String headerPattern,
					  final String footerPattern) {
		super(charset);
		layout = PatternLayout.newBuilder()
				.withConfiguration(config)
				.withPattern(eventPattern)
				.withRegexReplacement(replace)
				.withPatternSelector(patternSelector)
				.withCharset(charset)
				.withAlwaysWriteExceptions(alwaysWriteExceptions)
				.withDisableAnsi(disableAnsi)
				.withNoConsoleNoAnsi(noConsoleNoAnsi)
				.withHeader(headerPattern)
				.withFooter(footerPattern)
				.build();
	}

	@PluginFactory
	public static IbisPatternLayout createLayout(
			@PluginAttribute(value = "pattern", defaultString = PatternLayout.DEFAULT_CONVERSION_PATTERN) final String pattern,
			@PluginElement("PatternSelector") final PatternSelector patternSelector,
			@PluginConfiguration final Configuration config,
			@PluginElement("Replace") final RegexReplacement replace,
			// LOG4J2-783 use platform default by default, so do not specify defaultString for charset
			@PluginAttribute(value = "charset") final Charset charset,
			@PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = true) final boolean alwaysWriteExceptions,
			@PluginAttribute(value = "noConsoleNoAnsi") final boolean noConsoleNoAnsi,
			@PluginAttribute("header") final String headerPattern,
			@PluginAttribute("footer") final String footerPattern) {
		return new IbisPatternLayout(config, replace, pattern, patternSelector, charset, alwaysWriteExceptions, false, noConsoleNoAnsi, headerPattern, footerPattern);
	}
}
