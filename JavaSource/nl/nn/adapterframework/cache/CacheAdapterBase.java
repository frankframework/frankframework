/*
 * $Log: CacheAdapterBase.java,v $
 * Revision 1.2  2010-09-20 15:48:41  L190409
 * added warning for empty key
 *
 * Revision 1.1  2010/09/13 13:28:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added cache facility
 *
 */
package nl.nn.adapterframework.cache;

import java.io.Serializable;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Baseclass for caching.
 * Provides key transformation functionality.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Cache, will be set from owner</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyXPath(String) keyXPath}</td><td>xpath expression to extract cache key from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyNamespaceDefs(String) keyNamespaceDefs}</td><td>namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyStyleSheet(String) keyStyleSheet}</td><td>stylesheet to extract cache key from message</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public abstract class CacheAdapterBase implements ICacheAdapter {
	protected Logger log = LogUtil.getLogger(this);

	private String name;

	private String keyXPath;
	private String keyNamespaceDefs;
	private String keyStyleSheet;

	private TransformerPool keyTp=null;

	public void configure(String ownerName) throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ownerName+"Cache");
		}
		if (StringUtils.isNotEmpty(getKeyXPath()) || StringUtils.isNotEmpty(getKeyStyleSheet())) {
			keyTp=TransformerPool.configureTransformer(getLogPrefix(),getKeyNamespaceDefs(), getKeyXPath(), getKeyStyleSheet(),"text",false,null);
		}
	}
	
	protected abstract Serializable getElement(String key);
	protected abstract void putElement(String key, Serializable value);

	public String transformKey(String input) {
		if (keyTp!=null) {
			try {
				String key=keyTp.transform(input, null);
				if (StringUtils.isEmpty(key)) {
					log.warn("determined empty cache key");
				}
				return key;
			} catch (Exception e) {
			   log.error(getLogPrefix()+"cannot determine cache key",e);
			}
		}
		return input;
	}
	
	public String getString(String key) {
		return (String)getElement(key);
	}
	public void putString(String key, String value) {
		putElement(key, value);
	}

	public Serializable get(String key){
		return getElement(key);
	}
	public void put(String key, Serializable value) {
		putElement(key, value);
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name=name;
	}

	public String getLogPrefix() {
		return "cache ["+getName()+"] ";
	}
	
	public String getKeyXPath() {
		return keyXPath;
	}
	public void setKeyXPath(String keyXPath) {
		this.keyXPath = keyXPath;
	}

	public String getKeyNamespaceDefs() {
		return keyNamespaceDefs;
	}
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		this.keyNamespaceDefs = keyNamespaceDefs;
	}

	public String getKeyStyleSheet() {
		return keyStyleSheet;
	}
	public void setKeyStyleSheet(String keyStyleSheet) {
		this.keyStyleSheet = keyStyleSheet;
	}
}
