package nl.nn.adapterframework.lifecycle;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * During the ApplicationContext refresh Spring will try and automatically create and register all EventListeners
 * EventListeners which implement this interface will be exempt from this behavior but in turn will need to be 
 * registered manually in the required org.springframework.context.ConfigurableApplicationContext.
 *
 * @param <T>
 */
public interface LazyLoadingEventListener<T extends ApplicationEvent> extends ApplicationListener<T> {

}
