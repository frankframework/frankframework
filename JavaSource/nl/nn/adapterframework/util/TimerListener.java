package nl.nn.adapterframework.util;

import java.util.EventListener;
/**
 * Generic Timer functionality
 * @version Id
 * @see Timer
 */
public interface TimerListener extends EventListener
{
	public static final String version="$Id: TimerListener.java,v 1.3 2004-03-26 10:42:39 NNVZNL01#L180564 Exp $";
	
    public void onTimeout();
}
