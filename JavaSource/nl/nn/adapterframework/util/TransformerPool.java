/*
 * $Log: TransformerPool.java,v $
 * Revision 1.1  2004-10-05 09:56:59  L190409
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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

/**
 * Pool of transformers. 
 * 
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	public static final String version = "$Id: TransformerPool.java,v 1.1 2004-10-05 09:56:59 L190409 Exp $";
	protected Logger log = Logger.getLogger(this.getClass());

	private TransformerFactory tFactory = TransformerFactory.newInstance();
	private Source source;
	private boolean includeXmlDeclaration = true;

	private ObjectPool pool = new GenericObjectPool(new BasePoolableObjectFactory() {
		public Object makeObject() throws Exception {
			return createTransformer();
		}
	}); 
	
	public TransformerPool(Source source) throws TransformerConfigurationException {
		super();
		this.source=source;
		
		// check if a transformer can be initiated
		Transformer t = getTransformer();
		releaseTransformer(t);
	}	

	public TransformerPool(URL url) throws TransformerConfigurationException, IOException {
		this(new StreamSource(url.openStream()));
		source.setSystemId(url.toString());
	}
	
	public TransformerPool(String xsltString) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)));
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
	
	
	public Transformer getTransformer() throws TransformerConfigurationException {
		try {
			return (Transformer)pool.borrowObject();
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}
	
	public void releaseTransformer(Transformer t) throws TransformerConfigurationException {
		try {
			pool.returnObject(t);
		} catch (Exception e) {
			throw new TransformerConfigurationException("exception returning transformer to pool", e);
		}
	}

	public void invalidateTransformer(Transformer t) throws Exception {
		pool.invalidateObject(t);
	}

    protected Transformer createTransformer() throws TransformerConfigurationException {
		Transformer t = tFactory.newTransformer(source);
		if (t==null) {
			throw new TransformerConfigurationException("cannot instantiate transformer from Source ["+source.getSystemId()+"]");
		}
		if (includeXmlDeclaration)
			t.setOutputProperty("omit-xml-declaration", "no");
		else 
			t.setOutputProperty("omit-xml-declaration","yes");
    	return t;
    }

	public void setIncludeXmlDeclaration(boolean b) {
		includeXmlDeclaration = b;
	}

	
}
