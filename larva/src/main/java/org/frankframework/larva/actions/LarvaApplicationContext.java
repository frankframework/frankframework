/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva.actions;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.core.NameAware;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.lifecycle.ConfigurableApplicationContext;

/**
 * Custom Larva SpringContext, used to load the Larva scenario actions.
 * Not a fan of the IbisContext being passed/used here, but for now it is the only way to get the `Application` SpringContext.
 */
@Log4j2
public class LarvaApplicationContext extends ConfigurableApplicationContext {

	public LarvaApplicationContext(IbisContext ibisContext, String scenarioDirectory) throws ClassLoaderException {
		log.debug("Creating LarvaApplicationContext for scenarioDirectory [{}]", scenarioDirectory);
		// Use DirectoryClassLoader to make it possible to retrieve resources (such as styleSheetName) relative to the scenarioDirectory.
		DirectoryClassLoader directoryClassLoader = new RelativePathDirectoryClassLoader();
		directoryClassLoader.setDirectory(scenarioDirectory);
		directoryClassLoader.setBasePath(".");
		directoryClassLoader.configure(null, "LarvaTool");
		setClassLoader(directoryClassLoader);

		setId("LarvaScenario");

		if (ibisContext != null) {
			setParent(ibisContext.getApplicationContext());
		}

		log.debug("Refreshing LarvaApplicationContext for scenarioDirectory [{}]", scenarioDirectory);
		refresh();
		log.debug("LarvaApplicationContext for scenarioDirectory [{}] has been created", scenarioDirectory);
	}

	/**
	 * Enables the {@link Autowired} annotation.
	 * Automatically set the name of the {@link NameAware} bean (for log purposes).
	 * Disables SSL capabilities by default on beans that implement {@link HasTruststore}.
	 */
	@Override
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		super.registerBeanPostProcessors(beanFactory);

		// Append @Autowired PostProcessor to allow automatic type-based Spring wiring.
		AutowiredAnnotationBeanPostProcessor postProcessor = new AutowiredAnnotationBeanPostProcessor();
		postProcessor.setAutowiredAnnotationType(Autowired.class);
		postProcessor.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(postProcessor);

		beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (bean instanceof NameAware object) { // Set the name for log purposes
					object.setName("Larva " + beanName);
				}

				if (bean instanceof HasTruststore base) { // Disable SSL capabilities by default
					base.setAllowSelfSignedCertificates(true);
					base.setVerifyHostname(false);
				}
				return bean;
			}
		});
	}
}
