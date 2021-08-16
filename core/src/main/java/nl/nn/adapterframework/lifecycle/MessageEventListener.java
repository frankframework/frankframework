package nl.nn.adapterframework.lifecycle;

import org.springframework.context.ApplicationListener;

import nl.nn.adapterframework.configuration.ConfigurationMessageEvent;
import nl.nn.adapterframework.configuration.IbisContext;

public class MessageEventListener implements ApplicationListener<ApplicationMessageEvent> {
	private static IbisContext ibisContext;

	public MessageEventListener() {
		System.out.println(this);
	}

	@Override
	public void onApplicationEvent(ApplicationMessageEvent event) {
		if(event instanceof ConfigurationMessageEvent) {
			String configurationName = ((ConfigurationMessageEvent) event).getSource().getName();
//			ibisContext.getMessageKeeper(configurationName).add(event.getMessageKeeperMessage());
		}
//		ibisContext.getMessageKeeper().add(event.getMessageKeeperMessage());

		System.out.println(event.getMessageKeeperMessage());
	}

	public static void setIbisContext(IbisContext ibisContext) {
		MessageEventListener.ibisContext = ibisContext;
	}
}
