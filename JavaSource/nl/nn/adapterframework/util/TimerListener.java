package nl.nn.adapterframework.util;

import java.util.EventListener;
/**
 * Generic Timer functionality
 * <p>$Id: TimerListener.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $</p>
 * @see Timer
 */
public interface TimerListener extends EventListener
{
	public static final String version="$Id: TimerListener.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $";
	
    public void onTimeout();
}
