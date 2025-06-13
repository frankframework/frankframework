/*
   Copyright 2021 WeareFrank!

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
package org.frankframework.jndi;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.LogUtil;

/**
 * Tomcat Resource Factory that looks up objects in a delegate JNDI.
 *
 * Configure resource in Tomcat context.xml like:
 * <pre>
 * 	&lt;Resource
		name="jms/qcf_tibco_esb_ff"
		factory="org.frankframework.jndi.TomcatJndiProxy"

		delegate_name="SLXHP_Queue_ConnectionFactory"
		delegate_jndiProperties="TibcoJndi.properties"
		delegate_providerURL="tibjmsnaming://DEVESBLARGEDC1:37243,tibjmsnaming://DEVESBLARGEDC2:37243"

		userName="IBIS_AWS_POC_USER"
		userPassword="xxxxxxx"
		SSLVendor="j2se"
		SSLEnableVerifyHost="false"
		SSLEnableVerifyHostName="false"
		SSLTrace="false"
		SSLDebugTrace="false"
	/&gt;
   </pre>
 * All attributes starting with the prefix 'delegate_' are used to configure the TomcatJndiProxy by
 * calling setters with corresponding names (without the prefix).
 * <br/>
 * The remaining attributes (except 'name' and 'factory') are used to configure the resulting object by
 * calling corresponding setters.
 * <br/>
 * In the exampe the username and password are set, to be used when the application uses the resulting
 * connectionFactory to create a connection.
 * <br/>
 * <br/>
 * TibcoJndi.properties is a classpath resource, containing for instance:
 * <pre>
 * java.naming.factory.initial=com.tibco.tibjms.naming.TibjmsInitialContextFactory
 * com.tibco.tibjms.naming.security_protocol=ssl
 * com.tibco.tibjms.naming.ssl_enable_verify_host=false
 * </pre>
 * Setting 'java.naming.factory.initial' here causes the TomcatJndiProxy to query the Tibco JNDI at (delegate_)providerURL.
 * <br/>
 *
 * @see "https://tomcat.apache.org/tomcat-8.0-doc/jndi-resources-howto.html#Adding_Custom_Resource_Factories"
 *
 * @author Gerrit van Brakel
 *
 * @param <C> the type of class that is looked up
 */
public class TomcatJndiProxy<C> extends JndiBase implements ObjectFactory {
	private @Getter @Setter String name;

	protected Logger log = LogUtil.getLogger(this);

	public static final String DELEGATE_PREFIX="delegate_";

	@Override
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
		try {
			Reference ref = (Reference)obj; // For Tomcat, obj will always be an object of type Reference

			String objectName = name.toString();
			String targetClassName = ref.getClassName();
			//Class targetClass = Class.forName(targetClassName);
			log.debug("constructing object [{}] of type [{}]", objectName, targetClassName);

			// fetch and set delegate properties
			for (Enumeration<RefAddr> refAddrEnum=ref.getAll(); refAddrEnum.hasMoreElements();) {
				RefAddr refAddr = refAddrEnum.nextElement();
				if (refAddr.getType().startsWith(DELEGATE_PREFIX)) {
					String propertyName = refAddr.getType().substring(DELEGATE_PREFIX.length());
					Object propertyValue = refAddr.getContent();
					//log.debug("setting delegate property [{}] to value [{}]", propertyName, propertyValue);
					BeanUtils.setProperty(this, propertyName, propertyValue);
				}
			}

			String targetObjectName = StringUtils.isNotEmpty(getName()) ? getName() : objectName; // if delegate_name is not specified, target object name defaults to 'name'
			Context context = getContext();
			Object result = context.lookup(targetObjectName);

			for (Enumeration<RefAddr> refAddrEnum=ref.getAll(); refAddrEnum.hasMoreElements();) {
				RefAddr refAddr = refAddrEnum.nextElement();
				String propertyName = refAddr.getType();
				if (!propertyName.startsWith(DELEGATE_PREFIX) && !"factory".equals(propertyName)) {
					Object propertyValue = refAddr.getContent();
					//log.debug("setting result property [{}] to value [{}]", propertyName, propertyValue);"
					if (!PropertyUtils.isWriteable(result, propertyName)) {
						log.warn("object of type [{}] with name [{}] has no property [{}]", result.getClass().getTypeName(), targetObjectName, propertyName);
					}
					BeanUtils.setProperty(result, propertyName, propertyValue);
				}
			}
			log.debug("looked up object [{}]", result);
			return result;
		} catch (Exception e) {
			log.warn("Could not lookup object [{}]", name, e);
			throw e;
		}
	}
}
