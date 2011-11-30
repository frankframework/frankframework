/*
 * $Log: Counter.java,v $
 * Revision 1.7  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2006/04/10 09:29:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clear() and setValue()
 *
 * Revision 1.4  2004/09/07 08:44:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added increase and decrease with amount-argument
 *
 */
package nl.nn.adapterframework.util;

/**
 * Synchronized Counter.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class Counter {
	public static final String version="$RCSfile: Counter.java,v $  $Revision: 1.7 $ $Date: 2011-11-30 13:51:49 $";
	
	private long value = 0 ;

	public Counter(int startValue) {
		super();
		value = startValue;
	}
	public synchronized long decrease() {
		return --value;
	}
	public synchronized long decrease(long amount) {
		return value-=amount;
	}
	public synchronized long increase() {
		return ++value;
	}
	public synchronized long increase(long amount) {
		return value+=amount;
	}
	public synchronized void clear() {
		value=0;	
	}
	public synchronized long getValue() {
		return value;
	}
	public synchronized void setValue(long newValue) {
		value=newValue;
	}
}
