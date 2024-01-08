/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.jms;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.frankframework.aws.AwsBase;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;

import lombok.Getter;
import lombok.Setter;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

public class AmazonSqsFactory extends AwsBase implements ObjectFactory {

	private @Getter @Setter String queues;

	@Override
	public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
		try {
			Reference ref = (Reference)obj; // For Tomcat, obj will always be an object of type Reference

			String objectName = name.toString();
			String targetClassName = ref.getClassName();
			//Class targetClass = Class.forName(targetClassName);
			log.debug("constructing object [{}] of type [{}]", objectName, targetClassName);

			// fetch and set properties
			for (Enumeration<RefAddr> refAddrEnum = ref.getAll(); refAddrEnum.hasMoreElements();) {
				RefAddr refAddr = refAddrEnum.nextElement();
				String propertyName = refAddr.getType();
				Object propertyValue = refAddr.getContent();

				log.debug("setting delegate property [{}] to value [{}]", propertyName, propertyValue);
				BeanUtils.setProperty(this, propertyName, propertyValue);
			}

			ConnectionFactory result = createConnectionFactory();
			log.debug("looked up ConnectionFactory [{}]", result);
			createQueues(result.createConnection(), getQueues());
			return result;
		} catch (Exception e) {
			log.warn("Could not lookup object [{}]", name, e);
			throw e;
		}
	}

	public ConnectionFactory createConnectionFactory() {
		ProviderConfiguration providerConfiguration = new ProviderConfiguration();

		SqsClient client = createSqsClient();

		SQSConnectionFactory connectionFactory = new SQSConnectionFactory(providerConfiguration, client);

		return connectionFactory;
	}

	// A dirty workaround to create queues, should use JmsMessagingSource#createDestination
	public void createQueues(Connection connection, String queues) throws JMSException {
		if (StringUtils.isNotEmpty(queues)) {
			AmazonSQSMessagingClientWrapper client = ((SQSConnection) connection).getWrappedAmazonSQSClient();
			String[] queueArr = queues.split(",");
			for (int i=0; i<queueArr.length; i++) {
				if (!client.queueExists(queueArr[i])) {
					log.debug("creating SQS queue [{}", queueArr[i]);
					client.createQueue(queueArr[i]);
				}
			}
		}
	}

	public SqsClient createSqsClient() {
		SqsClientBuilder builder = SqsClient.builder();
		builder.region(Region.of(getClientRegion()));
		builder.credentialsProvider(getAwsCredentialsProvider());
//		builder.endpointProvider(new SqsEndpointProvider()); // TODO allow the use of destinationName="https://sqs.eu-west-1.amazonaws.com/00123567890/dummy-ibis-test-queue"
		return builder.build();
	}
}
