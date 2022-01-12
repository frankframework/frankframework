package nl.nn.adapterframework.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Plugin(name = "IbisNdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"TC"})
public class IbisNdcPatternConverter extends LogEventPatternConverter {
	/**
	 * Singleton.
	 */
	private static final IbisNdcPatternConverter INSTANCE = new IbisNdcPatternConverter();

	/**
	 * Private constructor.
	 */
	private IbisNdcPatternConverter() {
		super("xx", "ndc");
	}

	/**
	 * Obtains an instance of NdcPatternConverter.
	 *
	 * @param options options, may be null.
	 * @return instance of NdcPatternConverter.
	 */
	public static IbisNdcPatternConverter newInstance(final String[] options) {
		return INSTANCE;
	}

	@Override
	@PerformanceSensitive("allocation")
	public void format(final LogEvent event, final StringBuilder toAppendTo) {
		if(event.getContextStack().isEmpty())
			toAppendTo.append("null");
		else
			toAppendTo.append(event.getContextStack());
	}
}
