/*
   Copyright 2019, 2020, 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.frankframework.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.security.cert.CertPathValidatorException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.logging.log4j.Logger;

import org.frankframework.cache.ICache;
import org.frankframework.cache.ICacheEnabled;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;

/**
 * Client for LDAP.<br/>
 *
 * consider setting the following properties:<br/>
 * - java.naming.provider.url<br/>
 * - com.sun.jndi.ldap.connect.pool<br/>
 * - java.naming.referral = follow<br/>
 * - nl.nn.iuf.LdapClient.groupAttributeCache.name<br/>
 * - nl.nn.iuf.LdapClient.groupAttributeCache.timeToLive<br/>
 * <br/>
 * <b><u>Connection Pooling:</u></b><br/>
 * To set connection pool properties that are set as custom properties,
 * create a properties file named <b>Ldap.properties</b> with the attributes and
 * place in the classpath. To override the name of the properties file or to
 * locate the properties within some relative location to classpath, eg,
 * nl/nn/iuf/Ldap.properties or Ldap-highperformance.properties add JVM
 * custom property <b>ldap.props.file</b> to have the file name you want to have.<br/>
 * <br/>
 * Connection pooling is enabled by passing environment property :
 * <b>"com.sun.jndi.ldap.connect.pool"</b> with <b>"true"</b>.<br/>
 * <br/>
 * Supported attributes are as follows,<br/>
 * - com.sun.jndi.ldap.connect.pool.authentication<br/>
 * - com.sun.jndi.ldap.connect.pool.debug<br/>
 * - com.sun.jndi.ldap.connect.pool.initsize<br/>
 * - com.sun.jndi.ldap.connect.pool.maxsize<br/>
 * - com.sun.jndi.ldap.connect.pool.prefsize<br/>
 * - com.sun.jndi.ldap.connect.pool.protocol<br/>
 * - com.sun.jndi.ldap.connect.pool.timeout<br/>
 *
 * @see "http://docs.oracle.com/javase/jndi/tutorial/ldap/connect/config.html"
 *
 */
public class LdapClient implements ICacheEnabled<String,Set<String>> {
	protected static Logger log =  LogUtil.getLogger(LdapClient.class);

    private static final String DEFAULT_INITIAL_CONTEXT_FACTORY_NAME = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String JNDI_AUTH_ALIAS_KEY = "jndiAuthAlias";
//    private String ATTRIBUTE_CACHE_JNDI_NAME_KEY = "attributeCache.jndiName";
//    private String ATTRIBUTE_CACHE_TIME_TO_LIVE_KEY = "attributeCache.timeToLive";
//    private int ATTRIBUTE_CACHE_TIME_TO_LIVE_DEFAULT = 3600;
    private static final String LDAP_PROPS_DEFAULT_FILENAME = "Ldap.properties";
    private static final String LDAP_PROPS_FILENAME_KEY = "ldap.props.file";
    private static final String LDAP_CONNECTION_POOL_TIMEOUT_DEFAULT = "600000"; // milliseconds that an idle connection may remain in the pool without being closed and removed

    //An Array of LDAP JVM custom properties for connection pooling with default values to set
    private static final String[][] LDAP_JVM_PROPS = {
    	{"com.sun.jndi.ldap.connect.pool.authentication", "none simple"},
    	{"com.sun.jndi.ldap.connect.pool.debug", null},
    	{"com.sun.jndi.ldap.connect.pool.initsize", "1"},
    	{"com.sun.jndi.ldap.connect.pool.maxsize", "20"},
    	{"com.sun.jndi.ldap.connect.pool.prefsize", null},
    	{"com.sun.jndi.ldap.connect.pool.protocol", "plain ssl"},
    	{"com.sun.jndi.ldap.connect.pool.timeout", LDAP_CONNECTION_POOL_TIMEOUT_DEFAULT}
    };

	private Hashtable<String,Object> jndiEnv=null;
	private ICache<String,Set<String>> attributeCache=null;

    static{
    	//set JVM custom properties from Ldap.properties only once
    	//check if a custom property exists to override LDAP properties name/location in classpath
    	String strPropsFileName = System.getProperty(LDAP_PROPS_FILENAME_KEY);
    	//if not available make the properties file name LDAP.properties
    	if(strPropsFileName == null){
    		strPropsFileName = LDAP_PROPS_DEFAULT_FILENAME;
    	}
    	setLdapJvmProperties(strPropsFileName);
    }

    public LdapClient() {

    }
    public LdapClient(Map<String,Object> options) {
    	super();
    	jndiEnv=getJndiEnv(options.entrySet());
//    	initCache((String)options.get(ATTRIBUTE_CACHE_JNDI_NAME_KEY),(String)options.get(ATTRIBUTE_CACHE_TIME_TO_LIVE_KEY));
    }

    public LdapClient(Properties options) {
    	super();
    	jndiEnv=getJndiEnv(options.entrySet());
//    	initCache((String)options.get(ATTRIBUTE_CACHE_JNDI_NAME_KEY),(String)options.get(ATTRIBUTE_CACHE_TIME_TO_LIVE_KEY));
    }


    protected void configure() throws ConfigurationException {
		if (attributeCache!=null) {
			attributeCache.setName("ldapAttributeCache_cache");
			attributeCache.configure();
		}
    }

	public void open() {
		if (attributeCache!=null) {
			attributeCache.open();
		}
	}

	public void close() {
		if (attributeCache!=null) {
			attributeCache.close();
		}
	}

	@Override
	public void setCache(ICache<String,Set<String>> cache) {
		attributeCache=cache;
	}
	@Override
	public ICache<String,Set<String>> getCache() {
		return attributeCache;
	}

	@SuppressWarnings("unchecked")
	protected Hashtable<String,Object> getJndiEnv(@SuppressWarnings("rawtypes") Set optionSet) {
		Hashtable<String,Object> jndiEnv = new Hashtable<>();

		//jndiEnv.put("com.sun.jndi.ldap.trace.ber", System.err);//ldap response in log for debug purposes
		jndiEnv.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY_NAME);
		for(Entry<String,Object> entry:(Set<Entry<String,Object>>)optionSet) {
			if (entry.getKey().equals(JNDI_AUTH_ALIAS_KEY)) {
				CredentialFactory jndiCf = new CredentialFactory((String)entry.getValue());
				if (jndiCf.getUsername()!=null)
					jndiEnv.put(Context.SECURITY_PRINCIPAL, jndiCf.getUsername());
				if (jndiCf.getPassword()!=null)
					jndiEnv.put(Context.SECURITY_CREDENTIALS, jndiCf.getPassword());
			} else {
				jndiEnv.put(entry.getKey(), entry.getValue());
			}
		}

		if (log.isDebugEnabled()) {
			for(String key:jndiEnv.keySet()) {
				String value=(String)jndiEnv.get(key);
				log.debug("jndiEnv [{}] = [{}]", key, key.equals(Context.SECURITY_CREDENTIALS) ? "********" : value);
			}
		}
		return jndiEnv;
	}

    /**
     *  Gets the Context<br/>
     *  When InitialContextFactory and ProviderURL are set, these are used
     *  to get the <code>Context</code>. Otherwise the the InitialContext is
     *  retrieved without parameters.<br/>
     *  <b>Notice:</b> you can set the parameters on the commandline with <br/>
     *  java -Djava.naming.factory.initial= xxx -Djava.naming.provider.url=xxx
     * <br/><br/>
     *
     * @return                                   The context value
     * @exception  javax.naming.NamingException  Description of the Exception
     */
    public DirContext getContext() throws NamingException {
    	try {
    		return new InitialDirContext(jndiEnv);
    	} catch (NamingException ne) {
    		for (Throwable cause=ne; cause!=null; cause=cause.getCause()) {
    			if (cause instanceof CertPathValidatorException cpve) {
					log.warn("CertPathValidatorException index [{}] certpath [{}]", cpve.getIndex(), cpve.getCertPath());
    			}
    		}
     		throw ne;
    	}
    }

    private String arrayToString(String[] values, String separator) {
    	if (values==null || values.length==0) {
    		return "";
    	}
    	String result=values[0];
    	for (int i=1;i<values.length; i++) {
    		result+=separator+values[i];
    	}
    	return result;
    }

    public NamingEnumeration<SearchResult> search(DirContext context, String searchDN, String filter, String returnedAttribute, int scope) throws NamingException {
    	String[] returnedAttributes=returnedAttribute==null?null:new String[] {returnedAttribute};
		return search(context,searchDN, filter, returnedAttributes, scope);
    }

    public NamingEnumeration<SearchResult> search(DirContext context, String searchDN, String filter, Set<String> returnedAttributes, int scope) throws NamingException {
     	String[] returnedAttributesArr=returnedAttributes==null?null:returnedAttributes.toArray(new String[returnedAttributes.size()]);
 		return search(context,searchDN, filter, returnedAttributesArr, scope);
    }

    public NamingEnumeration<SearchResult> search(DirContext context, String searchDN, String filter, String[] returnedAttributes, int scope) throws NamingException {
    	if (log.isDebugEnabled())
			log.debug("searchDN [{}] filter [{}] no params returnedAttributes [{}]", searchDN, filter, arrayToString(returnedAttributes, ","));
		SearchControls sc = new SearchControls();
		sc.setSearchScope(scope);
		if (returnedAttributes!=null) {
			sc.setReturningAttributes(returnedAttributes);
		}
		return context.search(searchDN, filter, sc);
    }


    public NamingEnumeration<SearchResult> searchSubtree(DirContext context, String searchDN, String filter, String param, Set<String> returnedAttributes) throws NamingException {
    	return searchSubtree(context, searchDN, filter, new Object[] {param},returnedAttributes.toArray(new String[returnedAttributes.size()]));
    }
    public NamingEnumeration<SearchResult> searchSubtree(DirContext context, String searchDN, String filter, String param, String returnedAttribute) throws NamingException {
    	return searchSubtree(context, searchDN, filter, new Object[] {param},returnedAttribute);
    }
    public NamingEnumeration<SearchResult> searchSubtree(DirContext context, String searchDN, String filter, Object[] params, String returnedAttribute) throws NamingException {
    	return searchSubtree(context,searchDN,filter,params,returnedAttribute==null?null:new String[] {returnedAttribute});
    }

    public NamingEnumeration<SearchResult> searchSubtree(DirContext context, String searchDN, String filter, Object[] params, String[] returnedAttributes) throws NamingException {
    	if (log.isDebugEnabled())
			log.debug("searchDN [{}] filter [{}] params {}]", searchDN, filter, params == null ? "null" : params.length == 1 ? "[" + params[0] : " length [" + params.length);
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		if (returnedAttributes!=null) {
			sc.setReturningAttributes(returnedAttributes);
		}
		return context.search(searchDN, filter, params, sc);
    }

    public Set<String> searchRecursivelyViaAttributes(String uid, String baseDn, String attribute) throws NamingException {
		Set<String> results;
		Set<String> searched = new LinkedHashSet<>();

       	DirContext context=getContext();
       	try {
			int nestingLevel=0;
			if (log.isDebugEnabled()) log.debug("primary lookup of attribute [{}] of [{}]", attribute, uid);
			results=searchObjectForMultiValuedAttribute(context, uid, baseDn, attribute);
			Set<String> toBeSearched = new LinkedHashSet<>(results);
			while (!toBeSearched.isEmpty()) {
		       	Set<String> searchingNow=toBeSearched;
		       	toBeSearched=new LinkedHashSet<>();
		       	nestingLevel++;
				if (log.isDebugEnabled()) log.debug("secondary lookup of memberships of [{}] nestingLevel [{}]", uid, nestingLevel);
				for (String target:searchingNow) {
					if (log.isDebugEnabled())
						log.debug("secondary lookup of memberships of [{}] nestingLevel [{}], now searching [{}]", uid, nestingLevel, target);
					searched.add(target);
					Set<String> secondaryResults=searchObjectForMultiValuedAttributeWithCache(context, target, baseDn, attribute, true);
					for(String secondaryResult:secondaryResults) {
			    		if (!results.contains(secondaryResult)) {
			    			if (log.isDebugEnabled()) log.debug("nestingLevel [{}] found secondary membership [{}]", nestingLevel, secondaryResult);
			    			results.add(secondaryResult);
			    			if (!searched.contains(secondaryResult) &&
			    				!searchingNow.contains(secondaryResult)) {
			    				toBeSearched.add(secondaryResult);
			    			}
			    		}
					}
				}
			}
			return results;
       	} finally {
       		context.close();
       	}
    }

    /**
     * Search LDAP without filter, for example to find attributes of a specific user/object.
     */
    public Map<String,String> searchObjectForMultipleAttributes(String objectDN, String baseDn, Set<String> attributes) throws NamingException {
       	DirContext context=getContext();
       	try {
          	String filter="(objectClass=*)"; // filter cannot be empty
           	String relativeDN=objectDN.substring(0,objectDN.length()-baseDn.length()-1);
      		return getAttributeMap(search(context, relativeDN, filter, attributes, SearchControls.OBJECT_SCOPE));
       	} finally {
       		context.close();
       	}
	}

    public Map<String,List<String>> searchObjectForMultipleMultiValuedAttributes(String objectDN, String baseDn, Set<String> attributes) throws NamingException {
       	DirContext context=getContext();
       	try {
          	String filter="(objectClass=*)"; // filter cannot be empty
           	String relativeDN=objectDN.substring(0,objectDN.length()-baseDn.length()-1);
      		return getAttributeMultiMap(search(context, relativeDN, filter, attributes, SearchControls.OBJECT_SCOPE));
       	} finally {
       		context.close();
       	}
	}

    protected Set<String> searchObjectForMultiValuedAttributeWithCache(DirContext context, String objectDN, String baseDn, String attribute, boolean cacheNullResultsAsEmptySet) throws NamingException {
		if (attributeCache!=null) {
			String cacheKey=objectDN+"/"+attribute;
			Set<String> results=attributeCache.get(cacheKey);
			if (results==null) {
				results=searchObjectForMultiValuedAttribute(context, objectDN,baseDn,attribute);
				if (results==null) {
					if (cacheNullResultsAsEmptySet) {
						results=new HashSet<>();
					} else {
		    			if (log.isDebugEnabled()) log.debug("no attribute [{}] found for object [{}], will not cache", attribute, objectDN);
					}
				}
				if (results!=null) {
					if (log.isDebugEnabled()) log.debug("caching set of [{}] items of attribute [{}] for object [{}]", results.size(), attribute, objectDN);
					attributeCache.put(cacheKey, results);
				}
			}
			return results;
		}
		return searchObjectForMultiValuedAttribute(context, objectDN,baseDn,attribute);
    }

   /**
     * Search LDAP without filter, for example to find attributes of a specific user/object.
     */
    public Set<String> searchObjectForMultiValuedAttribute(String objectDN, String baseDn, String attribute) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return searchObjectForMultiValuedAttribute(context, objectDN, baseDn, attribute);
       	} finally {
       		context.close();
       	}
	}

    protected Set<String> searchObjectForMultiValuedAttribute(DirContext context, String objectDN, String baseDn, String attribute) throws NamingException {
    	return getAttributeSet(searchInObject(context, objectDN, baseDn, attribute));
    }

    public String searchObjectForSingleAttributeWithCache(String objectDN, String baseDn, String attribute) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return searchObjectForSingleAttributeWithCache(context, objectDN, baseDn, attribute);
       	} finally {
       		context.close();
       	}
    }
    protected String searchObjectForSingleAttributeWithCache(DirContext context, String objectDN, String baseDn, String attribute) throws NamingException {
    	Set<String> resultSet=searchObjectForMultiValuedAttributeWithCache(context, objectDN, baseDn, attribute, true);
    	if (resultSet!=null) {
    		Iterator<String> it = resultSet.iterator();
    		if (it.hasNext()) {
    			return it.next();
    		}
    	}
    	return null;
    }
    /**
     * Search LDAP without filter, for example to find attributes of a specific user/object.
     */
    public String searchObjectForSingleAttribute(String objectDN, String baseDn, String attribute) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return searchObjectForSingleAttribute(context, objectDN, baseDn, attribute);
       	} finally {
       		context.close();
       	}
	}

    public String searchObjectForSingleAttribute(DirContext context, String objectDN, String baseDn, String attribute) throws NamingException {
   		return getFirstSearchResult(searchInObject(context, objectDN, baseDn, attribute));
	}

    public NamingEnumeration<SearchResult> searchInObject(DirContext context, String objectDN, String baseDn, String attribute) throws NamingException {
       	String filter="(objectClass=*)"; // filter cannot be empty
       	String relativeDN=objectDN.substring(0,objectDN.length()-baseDn.length()-1);
   		return search(context, relativeDN, filter, attribute, SearchControls.OBJECT_SCOPE);
	}
    /**
     * Search LDAP for an object in some group (specified by the filter), for example to search for a user/object.
     */
    public Map<String,String> searchSubtreeForMultipleAttributes(String searchDN, String filter, String param, Set<String> attributes) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return getAttributeMap(searchSubtree(context, searchDN, filter, param, attributes));
       	} finally {
       		context.close();
       	}
	}

    public Set<String> searchSubtreeForMultiValuedAttribute(String searchDN, String filter, String param, String attribute) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return getAttributeSet(searchSubtree(context, searchDN, filter, param, attribute));
       	} finally {
       		context.close();
       	}
	}
    public Map<String,List<String>> searchSubtreeForMultipleMultiValuedAttributes(String searchDN, String filter, String param, Set<String> attributes) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return getAttributeMultiMap(searchSubtree(context, searchDN, filter, param, attributes));
       	} finally {
       		context.close();
       	}
	}
    public String searchSubtreeForSingleAttribute(String searchDN, String filter, String param, String attribute) throws NamingException {
       	DirContext context=getContext();
       	try {
       		return getFirstSearchResult(searchSubtree(context, searchDN, filter, param, attribute));
       	} finally {
       		context.close();
       	}
	}

	public String getFirstSearchResult(NamingEnumeration<SearchResult> searchResultEnum) throws NamingException {
		String result = null;
		try {
			if (searchResultEnum.hasMore()) {
				result = getFirstAttribute(searchResultEnum.next());
			}
		} catch (PartialResultException e) {
			log.debug("ignoring Exception: {}", e::getMessage);
		} finally {
			searchResultEnum.close();
		}
		return result;
	}

    public String getFirstAttribute(SearchResult searchResult) throws NamingException {
		Attributes attributes=searchResult.getAttributes();
		NamingEnumeration<? extends Attribute> attrenum= attributes.getAll();
		try {
			while (attrenum.hasMore()) {
				Attribute attr=attrenum.next();
				return (String)attr.get();
			}
			return null;
		} finally {
			attrenum.close();
		}
    }

    /**
     * Returns a Set of attribute values. The key of the attributes is ignored.
     */
    public Set<String> getAttributeSet(NamingEnumeration<SearchResult> searchResultEnum) throws NamingException {
    	Set<String> result=new LinkedHashSet<>();
    	mapMultiValuedAttribute(searchResultEnum, new AbstractObjectCallback<>(result) {

			@Override
			public void handle(Attribute key, Object value) {
				getData().add((String) value);
			}

		});
		return result;
    }

    /**
     * Returns a Map of attribute values. Only the first (or maybe the last...) value of each attribute is returned.
     */
    public Map<String,String> getAttributeMap(NamingEnumeration<SearchResult> searchResultEnum) throws NamingException {
    	Map<String,String> result=new LinkedHashMap<>();
    	mapMultipleAttributes(searchResultEnum, new AbstractObjectCallback<>(result) {

			@Override
			public void handle(Attribute key, Object value) {
				getData().put(key.getID(), (String) value);
			}

		});
		return result;
    }

    /**
     * Returns a MultiMap of attribute values.
     */
    public Map<String,List<String>> getAttributeMultiMap(NamingEnumeration<SearchResult> searchResultEnum) throws NamingException {
    	Map<String,List<String>> result=new LinkedHashMap<>();
    	mapMultiValuedAttribute(searchResultEnum, new AbstractObjectCallback<>(result) {

			@Override
			public void handle(Attribute key, Object value) {
				List<String> list = getData().computeIfAbsent(key.getID(), k -> new ArrayList<>());
				list.add((String) value);
			}

		});
		return result;
    }

    public void mapMultiValuedAttribute(NamingEnumeration<SearchResult> searchResultEnum, Callback<Attribute,Object> callback) throws NamingException {
    	try {
	    	while (searchResultEnum.hasMore()) {
	    		Attributes attributes=searchResultEnum.next().getAttributes();
	    		NamingEnumeration<? extends Attribute> attrenum=attributes.getAll();
	    		try {
	    			while (attrenum.hasMore()) {
	    				Attribute attr=attrenum.next();
	    	    		NamingEnumeration<?> multivalueattribute=attr.getAll();
	    	    		try {
	    	    			while (multivalueattribute.hasMore()) {
	    	    				callback.handle(attr,multivalueattribute.next());
	    	    			}
	    	    		} finally {
	    	    			multivalueattribute.close();
	    	    		}
	    			}
	    		} finally {
	    			attrenum.close();
	    		}
	    	}
		} catch(PartialResultException e) {
			log.debug("ignoring Exception: {}", e::getMessage);
		} finally {
			searchResultEnum.close();
		}
    }

    /**
     * runs a set of attribute values through a Mapper. Only the first value of each attribute is mapped.
     */
    public void mapMultipleAttributes(NamingEnumeration<SearchResult> searchResultEnum, Callback<Attribute,Object> callback) throws NamingException {
    	try {
	    	while (searchResultEnum.hasMore()) {
	    		Attributes attributes=searchResultEnum.next().getAttributes();
	    		NamingEnumeration<? extends Attribute> attrenum=attributes.getAll();
	    		try {
	    			while (attrenum.hasMore()) {
	    				Attribute attr=attrenum.next();
	    	    		NamingEnumeration<?> multivalueattribute=attr.getAll();
	    	    		try {
	    	    			if (multivalueattribute.hasMore()) {
	    	    				callback.handle(attr,multivalueattribute.next());
	    	    			}
	    	    		} finally {
	    	    			multivalueattribute.close();
	    	    		}
	    			}
	    		} finally {
	    			attrenum.close();
	    		}
	    	}
		} catch(PartialResultException e) {
			log.debug("ignoring Exception: {}", e::getMessage);
		} finally {
			searchResultEnum.close();
		}
    }

    public String checkPassword(String userDN, String password, String baseDN, String returnedAttribute) throws NamingException {

    	if (userDN==null || "".equals(userDN) || password==null || "".equals(password)) {
    		return null;
    	}
		Hashtable<String,Object> env = new Hashtable<>();
		env.putAll(jndiEnv);
		env.put("com.sun.jndi.ldap.connect.pool","false");
		env.put(Context.SECURITY_PRINCIPAL, userDN);
		env.put(Context.SECURITY_CREDENTIALS, password);

		// initiate private context, to avoid pooling of authentication
		InitialDirContext context = new InitialDirContext(env);
		try {
			return searchObjectForSingleAttribute(context,userDN,baseDN,returnedAttribute);
		} finally {
			context.close();
		}
    }

	public String authenticate(String username, String password, String searchDN, String baseDN, String searchFilter, String returnedAttributeDN, String returnedAttributeResult) throws NamingException {
		if (username==null|| "".equals(username) || password==null|| "".equals(password)) {
			return null;
		}
		String userDN=searchSubtreeForSingleAttribute(searchDN,searchFilter, username, returnedAttributeDN);
		if (userDN==null|| "".equals(userDN)) {
			return null;
		}
		return checkPassword(userDN, password, baseDN, returnedAttributeResult);
	}

	private static void setLdapJvmProperties(String resourceName){
		if (log.isDebugEnabled()) log.debug("[TAI] LDAP properties file [{}]", resourceName);

		Properties ldapProperties = new Properties();
		try {
			URL url = ClassLoaderUtils.getResourceURL(resourceName);
			if (url != null) {
				log.info("LDAP properties loading from file [{}]", url);
				try(InputStream is = StreamUtil.urlToStream(url, 10000); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
					ldapProperties.load(reader);
				}
			}

			for (String[] prop : LDAP_JVM_PROPS) {
				String property = prop[0];
				String propertyValue = ldapProperties.getProperty(property);
				if (propertyValue != null) {
					setLdapJvmProperty(property, propertyValue, false);
				} else {
					String defaultValue = prop[1];
					setLdapJvmProperty(property, defaultValue, true);
				}
			}

		} catch (IOException e) {
			log.error("Error reading LDAP properties", e);
		}
	}


	/**
	 * Set as JVM custom properties relevant for LDAP configuration supplied as options map for Login module.
	 * defaultVal == null does not set the custom property if unavailable in options Map.
	 *
	 * @param property custom property to set
	 * @param propValue value to set for the custom property
	 * @param isDefault when true, signals propValue to be set from a default
	 */
	private static void setLdapJvmProperty(String property, String propValue, boolean isDefault) {
		String currentValue = System.getProperty(property);

		if(propValue != null){
			if(currentValue == null){
				log.info("JVM custom property [{}] is set to {}[{}]", property, isDefault ? "default value " : "", propValue);
				System.setProperty(property, propValue);
			} else {
				if(!currentValue.equalsIgnoreCase(propValue)){
					log.warn("JVM custom property [{}] is overridden from [{}] to {}[{}]", property, currentValue, isDefault ? "default value " : "", propValue);
					System.setProperty(property, propValue);
				} else {
					if (log.isDebugEnabled()) log.debug("JVM custom property [{}] current value [{}] is not changed", property, currentValue);
				}
			}
		} else {
			if (log.isDebugEnabled())
				log.debug("JVM custom property [{}] current value [{}], no value or default specified", property, currentValue);
		}
	}

}
