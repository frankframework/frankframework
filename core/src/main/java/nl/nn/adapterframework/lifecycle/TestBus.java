package nl.nn.adapterframework.lifecycle;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@IbisInitializer
public class TestBus implements ApplicationContextAware {
	private AtomicInteger counter = new AtomicInteger();

	public String in() {
		int in = counter.incrementAndGet();
		System.out.println("in " + in);
		return ""+in;
	}
	public void out(String out) {
		System.out.println("out " + out);
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
//		SubscribableChannel channel = context.getBean("subscribableChannel", SubscribableChannel.class);
//
//		EventDrivenConsumer consumer = new EventDrivenConsumer(channel, this);

//		consumer.
	}
}
