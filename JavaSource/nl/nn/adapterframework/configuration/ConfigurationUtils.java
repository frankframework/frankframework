/*
 * $Log: ConfigurationUtils.java,v $
 * Revision 1.1  2010-05-19 10:27:51  m168309
 * first version
 *
 */
package nl.nn.adapterframework.configuration;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Functions to manipulate the configuration. 
 *
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class ConfigurationUtils {
	public static final String version = "$Id: ConfigurationUtils.java,v 1.1 2010-05-19 10:27:51 m168309 Exp $";

	private static final String CONFIGURATION_STUB4TESTTOOL_KEY = "stub4testtool.configuration";

	private static String stub4testtool_xslt = "/xml/xsl/stub4testtool.xsl";
	private static String active_xslt = "/xml/xsl/active.xsl";

	public static String getActivatedConfiguration(String originalConfig) throws ConfigurationException {
		URL active_xsltSource = ClassUtils.getResourceURL(ConfigurationUtils.class, active_xslt);
		if (active_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + active_xslt + "]");
		}
		try {
			Transformer active_transformer = XmlUtils.createTransformer(active_xsltSource);
			return XmlUtils.transformXml(active_transformer, originalConfig);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + active_xslt + "]", e);
		} catch (TransformerConfigurationException tce) {
			throw new ConfigurationException("got error creating transformer from file [" + active_xslt + "]", tce);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + active_xsltSource.toString() + "] from [" + active_xslt + "]", te);
		} catch (DomBuilderException de) {
			throw new ConfigurationException("caught DomBuilderException", de);
		}
	}

	public static String getStubbedConfiguration(String originalConfig) throws ConfigurationException {
		URL stub4testtool_xsltSource = ClassUtils.getResourceURL(ConfigurationUtils.class, stub4testtool_xslt);
		if (stub4testtool_xsltSource == null) {
			throw new ConfigurationException("cannot find resource [" + stub4testtool_xslt + "]");
		}
		try {
			Transformer active_transformer = XmlUtils.createTransformer(stub4testtool_xsltSource);
			return XmlUtils.transformXml(active_transformer, originalConfig);
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve [" + stub4testtool_xslt + "]", e);
		} catch (TransformerConfigurationException tce) {
			throw new ConfigurationException("got error creating transformer from file [" + stub4testtool_xslt + "]", tce);
		} catch (TransformerException te) {
			throw new ConfigurationException("got error transforming resource [" + stub4testtool_xsltSource.toString() + "] from [" + stub4testtool_xslt + "]", te);
		} catch (DomBuilderException de) {
			throw new ConfigurationException("caught DomBuilderException", de);
		}
	}

	public static boolean stubConfiguration() {
		return AppConstants.getInstance().getBoolean(CONFIGURATION_STUB4TESTTOOL_KEY, false);
	}
}