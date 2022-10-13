package nl.nn.adapterframework.testutil;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.mock.DirectQuerySenderMock;
import nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock;

/**
 * Enables the ability to provide a mockable FixedQuerySender.
 * 
 * @See {@link TestConfiguration#mockQuery(String, ResultSet)}
 * @See {@link FixedQuerySenderMock}
 * 
 * @author Niels Meijer
 */
public class QuerySenderPostProcessor implements BeanPostProcessor, ApplicationContextAware {
	private @Setter ApplicationContext applicationContext;
	private static Map<String, ResultSet> fixedQuerySenderMocks = new HashMap<>();
	private static Map<String, Message> fixedDirectSenderMocks = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if(FixedQuerySender.class.getCanonicalName().equals(bean.getClass().getCanonicalName())) {
			FixedQuerySenderMock qs = createMock(FixedQuerySenderMock.class);
			qs.addMockedQueries(fixedQuerySenderMocks);
			return qs;
		}
		if(DirectQuerySender.class.getCanonicalName().equals(bean.getClass().getCanonicalName())) {
			DirectQuerySenderMock qs = createMock(DirectQuerySenderMock.class);
			qs.addMockedQueries(fixedDirectSenderMocks);
			return qs;
		}

		return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
	}

	public void addFixedQuerySenderMock(String query, ResultSet resultSet) {
		fixedQuerySenderMocks.put(query, resultSet);
	}

	public void addDirectQuerySenderMock(String name, Message resultSet) {
		fixedDirectSenderMocks.put(name, resultSet);
	}

	@SuppressWarnings("unchecked")
	private <T> T createMock(Class<T> clazz) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}
}
