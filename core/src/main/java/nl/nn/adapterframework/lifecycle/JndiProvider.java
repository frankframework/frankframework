/*
   Copyright 2020 Nationale-Nederlanden

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

import nl.nn.adapterframework.extensions.cxf.NamespaceUriProvider;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.jndi.JndiObjectFactoryBean;

/**
 * 
 * @author Niels Meijer
 *
 */
@IbisInitializer
@DependsOn("determineApplicationServer")
public class JndiProvider implements ApplicationContextAware, InitializingBean {

	private Logger log = LogUtil.getLogger(this);
	private ApplicationContext applicationContext;
	private List<JndiObjectFactoryBean> jndiBeans;
	private String jndiContextPrefix = "java:";
	private String applicationServerType = null;
	public static String JNDI_BEAN_PREFIX = "JNDI-";

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		jndiBeans = new ArrayList<JndiObjectFactoryBean>();

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

		printContextList(); //traverse through the JNDI context
		
		JmsRealmFactory jmsFactory = JmsRealmFactory.getInstance();
		for(JndiObjectFactoryBean jndiObjectFactoryBean : jndiBeans) {

			JmsRealm jmsRealm = new JmsRealm();
			jmsRealm.setRealmName(jndiObjectFactoryBean.getJndiName());
			jmsRealm.setDatasourceName(jndiObjectFactoryBean.getJndiName());

			jmsFactory.registerJmsRealm(jmsRealm);
		}
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

//	private void printContext(Context ctx) throws Exception {
//		NamingEnumeration<Binding> en = ctx.listBindings("");
//		while (en.hasMore()) {
//			try {
//				Binding b = en.next();
//				if (b.getObject() instanceof Context) {
//					printContext((Context) b.getObject());
//				} else {
//					if(b.getObject() instanceof DataSource) {
//						System.out.println(b.isRelative());
//						System.out.println("Found DataSource: " + b.getName());
//						datasources.add((DataSource) b.getObject());
//					}
//				}
//			} catch (NamingException e) {
//				String error = e.getMessage();
//				if(error.startsWith("Class not found: ")) {
//					System.out.println("failed to load: " + error.substring(17));
//				} else 
//					e.printStackTrace();
//			}
//		}
//	}

	@Autowired
	public void setApplicationServerType(DetermineApplicationServer applicationServer) {
		this.applicationServerType = applicationServer.getApplicationServerType();
	}

	private void printContextList() throws Exception {
		printContextList(new InitialContext(), getJndiContextPrefix());
	}

	private void printContextList(InitialContext ictx, String jndiName) throws Exception {
		Object lookup = ictx.lookup(jndiName);
		if(lookup instanceof Context) {
			Context ctx = (Context) lookup;
			NamingEnumeration<NameClassPair> en = ctx.list("");
			while (en.hasMore()) {
				try {
					NameClassPair b = en.next();
					printContextList(ictx, jndiName+"/"+b.getName());
				} catch (NamingException e) {
					String error = e.getMessage();
					if(error.startsWith("Class not found: ")) {
						log.warn("failed to read JNDI on path ["+jndiName+"] class ["+error.substring(17)+"]");
						System.out.println("failed to read a JNDI on path ["+jndiName+"] class ["+error.substring(17)+"]");
					} else {
						log.warn("failed to read JNDI on path ["+jndiName+"]", e);
					}
				}
			}
		} else {
			JndiObjectFactoryBean bean = new JndiObjectFactoryBean();
			String beanName = jndiName.substring(jndiContextPrefix.length()+1);
			bean.setJndiName(jndiName);
			log.info("found JNDI on path ["+jndiName+"], autowiring as bean ["+beanName+"]");
			System.out.println("found JNDI on path ["+jndiName+"], autowiring as bean ["+beanName+"]");

			jndiBeans.add(bean);
			ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();

//			bean.setResourceRef(true); //Since we are providing the absolute url this is not necessarily
			AutowireCapableBeanFactory beanFactorya = applicationContext.getAutowireCapableBeanFactory();
//			beanFactorya.
			beanFactory.registerSingleton(beanName, bean); //Why are these 2 inverted?
			beanFactory.initializeBean(bean, beanName);
//			applicationContext.getAutowireCapableBeanFactory().createBean(bean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);
//			System.out.println("FOUND: " + lookup.getClass());
		}
	}

}
