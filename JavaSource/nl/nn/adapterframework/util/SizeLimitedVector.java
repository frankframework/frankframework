package nl.nn.adapterframework.util;

/**
 * Stores a maximum number of elements in a Vector.
 * If, after the maximum has exceeded, another element is put
 * in the vector the oldest element is removed.
 * <p>Creation date: (03-03-2003 9:10:43)</p>
 * <p>$Id: SizeLimitedVector.java,v 1.2 2004-02-04 10:02:04 a1909356#db2admin Exp $</p>
 * @author Johan Verrips
 */
public class SizeLimitedVector extends java.util.Vector {
	public static final String version="$Id: SizeLimitedVector.java,v 1.2 2004-02-04 10:02:04 a1909356#db2admin Exp $";
	
	private int maxSize=Integer.MAX_VALUE;
/**
 * SizeLimitedVector constructor comment.
 */
public SizeLimitedVector() {
	super();
}
	public SizeLimitedVector(int maxSize){
		this.maxSize=maxSize;	
	}
	public boolean  add(Object o){
		super.add(o);
		if (super.size()>maxSize) super.removeElementAt(0);
		return true;
	}
	public int getMaxSize() {
		return maxSize;
	}
	/**
	 * sets the Maximum Size to maxSize. If the current size
	 * is greater than the maximum size, the top elements are removed.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize=maxSize;
		if (this.size()>0){
			while (size()>maxSize) removeElementAt(0);
		}
		
	}
}
