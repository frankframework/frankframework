/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.larva.queues;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.core.IConfigurable;
import org.frankframework.larva.LarvaTool;
import org.frankframework.larva.TestConfig;
import org.frankframework.senders.FrankSender;

@Log4j2
public class QueueCreator {

	public static final String CLASS_NAME_PROPERTY_SUFFIX = ".className";
	private final TestConfig config;
	private final LarvaTool testTool;

	public QueueCreator(TestConfig config, LarvaTool testTool) {
		this.config = config;
		this.testTool = testTool;
	}

	public Map<String, Queue> openQueues(String scenarioDirectory, Properties properties, IbisContext ibisContext, String correlationId) {
		Map<String, Queue> queues = new HashMap<>();
		debugMessage("Get all queue names");

		try {
			// Use DirectoryClassLoader to make it possible to retrieve resources (such as styleSheetName) relative to the scenarioDirectory.
			DirectoryClassLoader directoryClassLoader = new RelativePathDirectoryClassLoader();
			directoryClassLoader.setDirectory(scenarioDirectory);
			directoryClassLoader.setBasePath(".");
			directoryClassLoader.configure(null, "LarvaTool");

			Set<String> queueNames = properties.keySet()
					.stream()
					.map(String.class::cast)
					.filter(key -> key.endsWith(CLASS_NAME_PROPERTY_SUFFIX))
					.map(key -> key.substring(0, key.lastIndexOf(".")))
					.collect(Collectors.toSet());

			for (String queueName : queueNames) {
				debugMessage("queuename openqueue: " + queueName);
				String className = properties.getProperty(queueName + CLASS_NAME_PROPERTY_SUFFIX);
				if ("org.frankframework.jms.JmsListener".equals(className)) {
					className = "org.frankframework.jms.PullingJmsListener";
				}

				IConfigurable configurable = QueueUtils.createInstance(ibisContext, directoryClassLoader, className);
				log.debug("created FrankElement [{}]", configurable);
				if (configurable instanceof FrankSender frankSender) {
					frankSender.setIbisManager(ibisContext.getIbisManager());
				}

				Properties queueProperties = handleDeprecations(QueueUtils.getSubProperties(properties, queueName), queueName);
				Queue queue = QueueWrapper.create(configurable, queueProperties, config.getTimeout(), correlationId);

				queue.configure();
				queue.open();
				queues.put(queueName, queue);
				debugMessage("Opened [" + className + "] '" + queueName + "'");
			}

		} catch (Exception e) {
			log.warn("Error occurred while creating queues", e);
			closeQueues(queues, properties, null);
			queues = null;
			errorMessage(e.getClass().getSimpleName() + ": "+e.getMessage(), e);
		}

		return queues;
	}

	private Properties handleDeprecations(Properties queueProperties, String keyBase) {
		if (queueProperties.containsKey("requestTimeOut") || queueProperties.containsKey("responseTimeOut")) {
			warningMessage("Deprecation Warning: properties " + keyBase + ".requestTimeOut/" + keyBase + ".responseTimeOut have been replaced with " + keyBase + ".timeout");
		}
		if (queueProperties.containsKey("getBlobSmart")) {
			warningMessage("Deprecation Warning: property " + keyBase + ".getBlobSmart has been replaced with " + keyBase + ".blobSmartGet");
			String blobSmart = ""+queueProperties.remove("getBlobSmart");
			queueProperties.setProperty("blobSmartGet", blobSmart);
		}
		if (queueProperties.containsKey("preDel1")) {
			warningMessage("Removal Warning: property " + keyBase + ".preDel<index> has been removed without replacement");
		}
		if (queueProperties.containsKey("prePostQuery")) {
			warningMessage("Removal Warning: property " + keyBase + ".prePostQuery has been removed without replacement");
		}

		return queueProperties;
	}

	private void closeQueues(Map<String, Queue> queues, Properties properties, String correlationId) {
		testTool.closeQueues(queues, properties, correlationId);
	}

	private void debugMessage(String message) {
		testTool.debugMessage(message);
	}

	private void warningMessage(String message) {
		testTool.warningMessage(message);
	}

	private void errorMessage(String message, Exception e) {
		testTool.errorMessage(message, e);
	}
}
