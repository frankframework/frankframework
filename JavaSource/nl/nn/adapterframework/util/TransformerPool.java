/*
 * $Log: TransformerPool.java,v $
 * Revision 1.6  2004-12-20 15:11:56  NNVZNL01#L180564
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

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Pool of transformers. 
 * 
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	public static final String version = "$Id: TransformerPool.java,v 1.6 2004-12-20 15:11:56 NNVZNL01#L180564 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private TransformerFactory tFactory = TransformerFactory.newInstance();
	private Source source;

	private ObjectPool pool = new GenericObjectPool(new BasePoolableObjectFactory() {
		public Object makeObject() throws Exception {
			return createTransformer();
		}
	}); 

	public TransformerPool(StreamSource source, String sysId) throws TransformerConfigurationException {
		super();
		this.source=source;
		if (StringUtils.isNotEmpty(sysId)) {
			this.source.setSystemId(sysId);
			log.debug("setting systemId to ["+sysId+"]");
		}
		
		// check if a transformer can be initiated
		Transformer t = getTransformer();
		
		releaseTransformer(t);
	}	
	
	public TransformerPool(StreamSource source) throws TransformerConfigurationException {
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
		Transformer t = tFactory.newTransformer(source);


		if (t==null) {
			throw new TransformerConfigurationException("cannot instantiate transformer from Source ["+source.getSystemId()+"]");
		}
    	return t;
    }

	public String transform(Document d, ParameterList parameterList, ParameterResolutionContext prc)
		throws ParameterException, TransformerException, IOException {

		return transform(new DOMSource(d),parameterList,prc);
	}

	public String transform(String s, ParameterList parameterList, ParameterResolutionContext prc)
		throws ParameterException, TransformerException, IOException {

		Variant inputVar = new Variant(s);
		Source in = inputVar.asXmlSource();

		return transform(in,parameterList,prc);
	}
	
	public String transform(Source s, ParameterList parameterList, ParameterResolutionContext prc) throws ParameterException, TransformerException, IOException {
		Transformer transformer = getTransformer();

		try {	
			if (parameterList!=null && prc!=null) {
				XmlUtils.setTransformerParameters(transformer, prc.getValues(parameterList));
			}
			return XmlUtils.transformXml(transformer, s);
		} 
		catch (ParameterException pe) {
			invalidateTransformerNoThrow(transformer);
			throw pe;
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



}
