/*
 * $Log: Counter.java,v $
 * Revision 1.4  2004-09-07 08:44:09  L190409
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
	public static final String version="$Id: Counter.java,v 1.4 2004-09-07 08:44:09 L190409 Exp $";
	
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
	public synchronized long getValue() {
		return value;
	}
	public synchronized long increase() {
		return ++value;
	}
	public synchronized long increase(long amount) {
		return value+=amount;
	}
}
