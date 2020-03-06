package nl.nn.adapterframework.extensions.log4j;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.XmlLayout;

import java.nio.charset.Charset;

@Plugin(name = "IbisXmlLayout", category = "Core", elementType = "layout", printObject = true)
public class IbisXmlLayout extends IbisMaskingLayout {
	protected IbisXmlLayout(Charset charset, boolean compact, boolean complete) {
		super(charset);
		layout = XmlLayout.newBuilder().setCompact(compact).setComplete(complete).build();
		System.out.println("CREATED XML LAYOUT!!");
	}

	@PluginFactory
	public static IbisXmlLayout createLayout(
			// LOG4J2-783 use platform default by default, so do not specify defaultString for charset
			@PluginAttribute(value = "charset") final Charset charset,
			@PluginAttribute(value = "complete", defaultBoolean = false) final boolean complete,
			@PluginAttribute(value = "compact", defaultBoolean = false) final boolean compact) {
		return new IbisXmlLayout(charset, compact, complete);
	}
}
