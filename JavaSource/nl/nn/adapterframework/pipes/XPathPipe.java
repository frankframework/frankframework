/*
 * $Log: XPathPipe.java,v $
 * Revision 1.5  2004-08-31 13:19:58  a1909356#db2admin
 * Allow multithreading
 *
 * Revision 1.4  2004/08/03 12:28:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced embedded stylesheet with call to xmlutils.createxpathevaluator
 *
 * Revision 1.3  2004/05/05 09:30:53  Johan Verrips <johan.verrips@ibissource.org>
 * added sessionkey feature
 *
 * Revision 1.2  2004/04/27 11:42:40  unknown <unknown@ibissource.org>
 * Access properties via getters
 *
 * Revision 1.1  2004/04/27 10:52:17  unknown <unknown@ibissource.org>
 * Pipe that evaluates an xpath expression on the inpup
 * 
 */
package nl.nn.adapterframework.pipes;

import java.io.ByteArrayInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>Expression to evaluate</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>If specified, the result is put 
 * in the PipeLineSession under the specified key, and the result of this pipe will be 
 * the same as the input (the xml). If NOT specified, the result of the xpath expression 
 * will be the result of this pipe</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author J. Dekker
 * @version Id
 */
public class XPathPipe extends FixedForwardPipe {
	private String xpathExpression;
	private ObjectPool transformerPool;
	private String sessionKey=null;
	
	/* 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(xpathExpression))
			throw new ConfigurationException(getLogPrefix(null)+"xpathExpression must be filled");
						
		try {
			transformerPool = new GenericObjectPool(new BasePoolableObjectFactory() {
				public Object makeObject() throws Exception {
					return XmlUtils.createXPathEvaluator(getXpathExpression());
				}
			}); 
		}
		catch(Exception e) {
			throw new ConfigurationException(getLogPrefix(null)+"cannot create XPath-evaluator from ["+getXpathExpression()+"]",e);
		}
	}

	/** 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Object, PipeLineSession)
	 */
	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		String in = (String)input;
		String out = null; 
		
		if (! StringUtils.isEmpty(in)) {
			Transformer t = null;
			try {
				t = (Transformer)transformerPool.borrowObject();
				out = XmlUtils.transformXml(t, in);
			} 
			catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session)+"error during xsl transformation", e);
			}
			finally {
				try { transformerPool.returnObject(t); } catch(Exception e) {}
			}
		}
		if (StringUtils.isEmpty(getSessionKey())){
			return new PipeRunResult(getForward(), out);
		} else {
			session.put(getSessionKey(), out);
			return new PipeRunResult(getForward(), input);
		}
	}
	
	/**
	 * @return the xpath expression to evaluate
	 */
	public String getXpathExpression() {
		return xpathExpression;
	}

	/**
	 * @param xpathExpression the xpath expression to evaluate
	 */
	public void setXpathExpression(String xpathExpression) {
		this.xpathExpression = xpathExpression;
	}
	
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * The name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see nl.nn.adapterframework.core.PipeLineSession
	 */
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

}
