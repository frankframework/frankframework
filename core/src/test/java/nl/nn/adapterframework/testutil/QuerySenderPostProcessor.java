package nl.nn.adapterframework.testutil;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import lombok.Getter;
import nl.nn.adapterframework.jdbc.FixedQuerySender;

/**
 * 
 * @author Niels Meijer
 *
 */
public class QuerySenderPostProcessor implements BeanPostProcessor {
	private final String FixedQuerySenderClassName = FixedQuerySender.class.getName();
	private @Getter FixedQuerySenderMock mock = new FixedQuerySenderMock();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if(FixedQuerySenderClassName.equals(beanName)) {
			return mock;
		}

		return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
	}
}
