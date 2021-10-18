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
import nl.nn.adapterframework.jdbc.FixedQuerySender;

/**
 * Enables the ability to provide a mockable FixedQuerySender. 
 * 
 * @See {@link TestConfiguration#mockQuery(String, ResultSet)}
 * @See {@link FixedQuerySenderMock}
 * 
 * @author Niels Meijer
 */
public class QuerySenderPostProcessor implements BeanPostProcessor, ApplicationContextAware {
	private final String FixedQuerySenderClassName = FixedQuerySender.class.getName();
	private @Setter ApplicationContext applicationContext;
	private Map<String, ResultSet> mocks = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if(FixedQuerySenderClassName.equals(beanName)) {
			FixedQuerySenderMock qs = createMock();
			qs.addMockedQueries(mocks);
			return qs;
		}

		return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
	}

	public void addMock(String query, ResultSet resultSet) {
		mocks.put(query, resultSet);
	}

	private FixedQuerySenderMock createMock() {
		return (FixedQuerySenderMock) applicationContext.getAutowireCapableBeanFactory()
				.createBean(FixedQuerySenderMock.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}
}
