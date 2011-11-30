package nl.nn.adapterframework.util;

import java.util.EventListener;
/**
 * Generic Timer functionality
 * @version Id
 * @see Timer
 */
public interface TimerListener extends EventListener
{
	
    public void onTimeout();
}
