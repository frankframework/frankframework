package nl.nn.adapterframework.util;

import java.util.EventListener;
/**
 * Generic Timer functionality
 * @see Timer
 */
public interface TimerListener extends EventListener
{
	public static final String version="$Id: TimerListener.java,v 1.1 2004-02-04 08:36:07 a1909356#db2admin Exp $";
	
    public void onTimeout();
}
