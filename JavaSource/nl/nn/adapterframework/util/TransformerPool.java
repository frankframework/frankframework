/*
 * $Log: TransformerPool.java,v $
 * Revision 1.16  2007-05-08 16:02:19  europe\L190409
 * added transform() with result as parameter
 *
 * Revision 1.15  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.14  2007/02/05 15:05:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use SoftReferencePool
 *
 * Revision 1.13  2005/10/20 15:22:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added explicit namespaceAware-setting for direct-string transformation
 *
 * Revision 1.12  2005/06/13 11:49:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use separate version of stringToSource, optimized for single use
 *
 * Revision 1.11  2005/06/13 10:01:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimized for single transformation of strings
 *
 * Revision 1.10  2005/05/31 09:48:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * using stringToSource() and always clearing parameters
 *
 * Revision 1.9  2005/03/31 08:16:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * generalized Source
 *
 * Revision 1.8  2005/03/04 07:52:45  Johan Verrips <johan.verrips@ibissource.org>
 * Multi-threading caused problems with closed streams on creating transfomers.
 * Transformers are now instantiated by a Templates object, which solves this problem
 * and increases perfomance
 *
 * Revision 1.7  2005/01/10 08:56:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Xslt parameter handling by Maps instead of by Ibis parameter system
 *
 * Revision 1.6  2004/12/20 15:11:56  Johan Verrips <johan.verrips@ibissource.org>
 * Bugfix: systemID now properly handled. Transformer was created before system id was set.
 *
 * Revision 1.5  2004/10/26 16:26:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set UTF-8 as default inputstream encoding
 *
 * Revision 1.4  2004/10/19 15:27:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved transformation to pool
 *
 * Revision 1.3  2004/10/14 16:12:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made creation of Transformes synchronized
 *
 * Revision 1.2  2004/10/12 15:15:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected XmlDeclaration-handling
 *
 * Revision 1.1  2004/10/05 09:56:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TransformerPool
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Pool of transformers. As of IBIS 4.2.e the Templates object is used to improve
 * performance and work around threading problems with the api.  
 * 
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	public static final String version = "$RCSfile: TransformerPool.java,v $ $Revision: 1.16 $ $Date: 2007-05-08 16:02:19 $";
	protected Logger log = LogUtil.getLogger(this);

	private TransformerFactory tFactory = TransformerFactory.newInstance();

	private Templates templates;
	
	private ObjectPool pool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
		public Object makeObject() throws Exception {
			return createTransformer();			
		}
	}); 

	public TransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		super();
		if (StringUtils.isNotEmpty(sysId)) {
			source.setSystemId(sysId);
			log.debug("setting systemId to ["+sysId+"]");
		}
		templates=tFactory.newTemplates(source);

		// check if a transformer can be initiated
		Transformer t = getTransformer();
		
		releaseTransformer(t);
	}	
	
	public TransformerPool(Source source) throws TransformerConfigurationException {
		this(source,null);
	}	

	public TransformerPool(URL url) throws TransformerConfigurationException, IOException {
		this(new StreamSource(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING),url.toString());
	}
	
	public TransformerPool(String xsltString) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)));
	}

	public TransformerPool(String xsltString, String sysId) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId);
	}

	
	public void open() throws Exception {
	}
	
	public void close() {
		try {
			pool.clear();			
		} catch (Exception e) {
			log.warn("exception clearing transformerPool",e);
		}
	}
	
	
	protected Transformer getTransformer() throws TransformerConfigurationException {
		try {
			return (Transformer)pool.borrowObject();
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}
	
	protected void releaseTransformer(Transformer t) throws TransformerConfigurationException {
		try {
			pool.returnObject(t);
		} catch (Exception e) {
			throw new TransformerConfigurationException("exception returning transformer to pool", e);
		}
	}

	protected void invalidateTransformer(Transformer t) throws Exception {
		pool.invalidateObject(t);
	}

	protected void invalidateTransformerNoThrow(Transformer transformer) {
		try {
			invalidateTransformer(transformer);
			log.debug("Transformer was removed from pool as an error occured on the last transformation");
		} catch (Throwable t) {
			log.error("Error on removing transformer from pool", t);
		}
	}


    protected synchronized Transformer createTransformer() throws TransformerConfigurationException {
    	
		Transformer t = templates.newTransformer();

		if (t==null) {
			throw new TransformerConfigurationException("cannot instantiate transformer");
		}
    	return t;
    }

	public String transform(Document d, Map parameters)	throws TransformerException, IOException {
		return transform(new DOMSource(d),parameters);
	}

	public String transform(String s, Map parameters) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s),parameters);
	}

	public String transform(String s, Map parameters, boolean namespaceAware) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s, namespaceAware),parameters);
	}

	public String transform(Source s, Map parameters) throws TransformerException, IOException {
		Transformer transformer = getTransformer();

		try {	
			XmlUtils.setTransformerParameters(transformer, parameters);
			return XmlUtils.transformXml(transformer, s);
		} 
		catch (TransformerException te) {
			invalidateTransformerNoThrow(transformer);
			throw te;
		} 
		catch (IOException ioe) {
			invalidateTransformerNoThrow(transformer);
			throw ioe;
		} 
		finally {
			if (transformer != null) {
				try {
					releaseTransformer(transformer);
				} catch(Exception e) {
					log.warn("Exception returning transformer to pool",e);
				};
			}
		}
	}

	public void transform(Source s, Result r, Map parameters) throws TransformerException {
		Transformer transformer = getTransformer();

		try {	
			XmlUtils.setTransformerParameters(transformer, parameters);
			transformer.transform(s,r);
		} 
		catch (TransformerException te) {
			invalidateTransformerNoThrow(transformer);
			throw te;
		} 
		finally {
			if (transformer != null) {
				try {
					releaseTransformer(transformer);
				} catch(Exception e) {
					log.warn("Exception returning transformer to pool",e);
				};
			}
		}
	}


}
