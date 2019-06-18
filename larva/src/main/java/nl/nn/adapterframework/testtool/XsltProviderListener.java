package nl.nn.adapterframework.testtool;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * XSLT provider listener for the Test Tool.
 * 
 * @author Jaco de Groot
 */
public class XsltProviderListener {
	String filename;
	boolean fromClasspath = false;
	private int xsltVersion=0; // set to 0 for auto detect.
	boolean namespaceAware = true;
	TransformerPool transformerPool = null;
	String result;

	public void init() throws ListenerException {
		try {
			if (fromClasspath) {
				transformerPool = TransformerPool.getInstance(ClassUtils.getResourceURL(this, filename), getXsltVersion());
			} else {
				File file = new File(filename);
				transformerPool = TransformerPool.getInstance(file.toURI().toURL(), getXsltVersion());
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

	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
//		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
//		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion";
//		configWarnings.add(log, msg);
		xsltVersion=b?2:1;
	}

	/**
	 * Set namespace aware.
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

}
