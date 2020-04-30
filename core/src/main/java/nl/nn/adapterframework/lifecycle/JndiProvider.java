/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jndi.JndiObjectFactoryBean;

import nl.nn.adapterframework.util.LogUtil;

/**
 * 
 * @author Niels Meijer
 *
 */
@IbisInitializer
@DependsOn("determineApplicationServerBean")
public class JndiProvider implements ApplicationContextAware, InitializingBean {

	private Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext;
	private String jndiContextPrefix = "java:";
	private String applicationServerType = null;
	private InitialContext rootContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(applicationServerType == null) {
			throw new Exception("Unable to determine application server type");
		}
		try {
			ContextPrefix prefix = ContextPrefix.valueOf(applicationServerType);
			jndiContextPrefix = prefix.getPrefix();
		} catch (IllegalArgumentException e) {
			log.warn("unable to determine JndiContextPrefix, using default ["+jndiContextPrefix+"]");
		}
		log.info("using JNDI ContextPrefix [" + jndiContextPrefix + "]");

		rootContext = new InitialContext();

		printContextList(); //traverse through the JNDI context
	}

	private enum ContextPrefix {
		JBOSS("java:/"),
		TOMCAT("java:comp/env");

		private String prefix = "";
		ContextPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getPrefix() {
			return prefix;
		}
	}

	public String getJndiContextPrefix() {
		return jndiContextPrefix;
	}

	@Autowired
	public void setApplicationServerType(DetermineApplicationServerBean applicationServer) {
		this.applicationServerType = applicationServer.getApplicationServerType();
	}

	private void printContextList() throws Exception {
		traverseJndiContext(getJndiContextPrefix());
	}

	public JndiObjectFactoryBean lookup(String jndiName) throws NamingException {
		if(!jndiName.startsWith(jndiContextPrefix)) 
			jndiName = getJndiContextPrefix() + "/" + jndiName;

		Object lookup = rootContext.lookup(jndiName);
		if(lookup == null) {
			log.warn("JNDI ["+jndiName+"] not found");
			return null;
		}

		if(lookup instanceof Context) {
			log.warn("JNDI ["+jndiName+"] is not a resource but a JNDI Context");
			return null;
		}

		JndiObjectFactoryBean bean = new JndiObjectFactoryBean();
		String beanName = jndiName.substring(jndiContextPrefix.length()+1);
		bean.setJndiName(jndiName);
		log.info("found JNDI on path ["+jndiName+"], wiring as bean ["+beanName+"]");

		Class<?> jndiClass = lookup.getClass().getClass();
		if(DataSource.class.isAssignableFrom(jndiClass)) {
			bean.setProxyInterface(DataSource.class);
			bean.setLookupOnStartup(false);
		} else if(XADataSource.class.isAssignableFrom(jndiClass)) {
			bean.setProxyInterface(XADataSource.class);
			bean.setLookupOnStartup(false);
		}

//		JmsRealm jmsRealm = new JmsRealm();
//		jmsRealm.setRealmName(beanName);
//		jmsRealm.setDatasourceName(beanName);
//		JmsRealmFactory.getInstance().registerJmsRealm(jmsRealm);

		registerAndInitializeBean(beanName, bean);

		return bean;
	}

	private void registerAndInitializeBean(String beanName, Object bean) {
		ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

		beanFactory.registerSingleton(beanName, bean);
		beanFactory.initializeBean(bean, beanName); //Why are these 2 arguments inverted?
	}

	private void traverseJndiContext(String jndiName) throws NamingException {
		Object lookup = rootContext.lookup(jndiName);
		if(lookup instanceof Context) {
			Context ctx = (Context) lookup;
			NamingEnumeration<NameClassPair> en = ctx.list("");
			while (en.hasMore()) {
				NameClassPair b = en.next();
				String name = jndiName+"/"+b.getName();
				try {
					traverseJndiContext(name);
				} catch (NamingException e) {
					String error = e.getMessage();
					if(error.startsWith("Class not found: ")) {
						log.warn("failed to read JNDI ["+name+"] with class ["+error.substring(17)+"]", e);
					} else {
						log.warn("failed to read JNDI ["+name+"]", e);
					}
				}
			}
		} else {
			lookup(jndiName);
		}
	}
}
