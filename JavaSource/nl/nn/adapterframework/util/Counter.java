package nl.nn.adapterframework.util;

/**
 * Synchronized Counter
 * <p>$Id: Counter.java,v 1.2 2004-02-04 10:02:04 a1909356#db2admin Exp $</p>
 *
 * @author Gerrit van Brakel
 */
public class Counter {
	public static final String version="$Id: Counter.java,v 1.2 2004-02-04 10:02:04 a1909356#db2admin Exp $";
	
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
