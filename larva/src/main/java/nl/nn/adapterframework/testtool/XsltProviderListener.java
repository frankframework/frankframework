package nl.nn.adapterframework.testtool;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * XSLT provider listener for the Test Tool.
 * 
 * @author Jaco de Groot
 */
public class XsltProviderListener {
	String filename;
	boolean fromClasspath = false;
	boolean xslt2 = false;
	boolean namespaceAware = true;
	TransformerPool transformerPool = null;
	String result;

	public void init() throws ListenerException {
		try {
			if (fromClasspath) {
				transformerPool = new TransformerPool(ClassUtils.getResourceURL(filename), xslt2);
			} else {
				File file = new File(filename);
				StreamSource streamSource = new StreamSource(file);
				transformerPool = new TransformerPool(streamSource, xslt2);
			}
		} catch (Exception e) {
			throw new ListenerException("Exception creating transformer pool for file '" + filename + "': " + e.getMessage(), e);
		}
	}

	public void processRequest(String message, Map parameters) throws ListenerException {
		try {
			result = transformerPool.transform(message, parameters, namespaceAware);
		} catch (IOException e) {
			throw new ListenerException("IOException transforming xml: " + e.getMessage(), e);
		} catch (TransformerException e) {
			throw new ListenerException("TransformerException transforming xml: " + e.getMessage(), e);
		} catch (DomBuilderException e) {
			throw new ListenerException("DomBuilderException transforming xml: " + e.getMessage(), e);
		}
	}

	public String getResult() {
		String result = this.result;
		this.result = null;
		return result;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setFromClasspath(boolean fromClasspath) {
		this.fromClasspath = fromClasspath;
	}

	public void setXslt2(boolean xslt2) {
		this.xslt2 = xslt2;
	}

	/**
	 * Set namespace aware.
	 *  
	 * @param namespaceAware
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

}
