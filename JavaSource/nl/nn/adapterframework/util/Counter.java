package nl.nn.adapterframework.util;

/**
 * Synchronized Counter
 * @version Id
 *
 * @author Gerrit van Brakel
 */
public class Counter {
	public static final String version="$Id: Counter.java,v 1.3 2004-03-26 10:42:42 NNVZNL01#L180564 Exp $";
	
	private long value = 0 ;

/**
 * Counter constructor comment.
 */
public Counter() {
	super();
}
	public Counter(int startValue) {
		super();
		value = startValue;
	}
	public synchronized long decrease() {
		return --value;
	}
	public synchronized long getValue() {
		return value;
	}
	public synchronized long increase() {
		return ++value;
	}
}
