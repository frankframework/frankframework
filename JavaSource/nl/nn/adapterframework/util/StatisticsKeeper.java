package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Keeps statistics (min, max, count etc).
 * <p>Creation date: (19-02-2003 11:34:14)</p>
 * @version Id
 * @author Johan Verrips
 */
public class StatisticsKeeper {
	public static final String version="$Id: StatisticsKeeper.java,v 1.4 2004-07-22 11:13:16 L190409 Exp $";
	
	private String name = null;
	private long min = Integer.MAX_VALUE;
    private long max = 0;
    private long count = 0;
    private long total = 0;
    private long totalSquare=0;
	private long first=0;
    private long last=0;
    private long classBoundaries[];
    private long classCounts[];

   	// key that is looked up to retrieve texts to be signalled
	private static String statConfigKey="Statistics.boundaries";
 
 	public static final int NUM_STATIC_ITEMS=8;   
    public static final int ITEM_TYPE_INTEGER=1;
    public static final int ITEM_TYPE_TIME=2;
    public static final int ITEM_TYPE_FRACTION=3;
    public static final String DEFAULT_BOUNDARY_LIST="100,500,1000,5000";
/**
 * Constructor for StatisticsKeeper.
 *
 * Initializes statistics. Reads values for class boundaries from AppConstants
 *
 * @see AppConstants
 */
public StatisticsKeeper(String name) {
    super();
    this.name = name;
    ArrayList classBoundariesBuffer = new ArrayList();

    StringTokenizer tok = AppConstants.getInstance().getTokenizer(statConfigKey,DEFAULT_BOUNDARY_LIST);

    while (tok.hasMoreTokens()) {
        classBoundariesBuffer.add(new Long(Long.parseLong(tok.nextToken())));
    }
    classBoundaries = new long[classBoundariesBuffer.size()];
    classCounts = new long[classBoundariesBuffer.size()];
    for (int i = 0; i < classBoundariesBuffer.size(); i++) {
        classBoundaries[i] = ((Long) classBoundariesBuffer.get(i)).longValue();
    }

}
public StatisticsKeeper(StatisticsKeeper stat) {
	name = stat.name;
    min = stat.min;
    max = stat.max;
    count = stat.count;
    total = stat.total;
    totalSquare = stat.totalSquare;

    classBoundaries = stat.getClassBoundaries();
    classCounts = new long[classBoundaries.length];
}
public void addValue(long value) {
	if (count==0) { 
		first=value;
	}
    ++count;
    total += value;
    if (value > max) {
        max = value;
    }
    if (value < min) {
        min = value;
    }
    totalSquare += value * value;
    last = value;

    for (int i = 0; i < classBoundaries.length; i++) {
        if (value < classBoundaries[i]) {
            classCounts[i]++;
        }
    }

}
    public long getAvg()
    {
        if (count == 0)
        {
            return 0;
        }
        return (long)(total / count);
    }
public long[] getClassBoundaries() {
	return classBoundaries;
}
public long[] getClassCounts() {
	return classCounts;
}
    public long getCount()
    {
        return count;
    }
    public int getItemCount()
    {
        return NUM_STATIC_ITEMS+classBoundaries.length;
    }
    public String getItemName(int index)
    {
	    switch (index) {
		    case 0: return "count";
		    case 1: return "min";
		    case 2: return "max";
		    case 3: return "avg";
			case 4: return "stdDev";
			case 5: return "first";
			case 6: return "last";
		    case 7: return "total";
		    default : return "< "+classBoundaries[index-NUM_STATIC_ITEMS]+"ms";
	    }
    }
    public int getItemType(int index)
    {
	    switch (index) {
		    case 0: return ITEM_TYPE_INTEGER;
		    case 1: return ITEM_TYPE_TIME;
		    case 2: return ITEM_TYPE_TIME;
		    case 3: return ITEM_TYPE_TIME;
		    case 4: return ITEM_TYPE_TIME;
		    case 5: return ITEM_TYPE_TIME;
			case 6: return ITEM_TYPE_TIME;
		    case 7: return ITEM_TYPE_TIME;
		    default : return ITEM_TYPE_FRACTION;
	    }
    }
    public Object getItemValue(int index)
    {
	    switch (index) {
		    case 0: return new Long(getCount());
		    case 1: if (getCount() == 0) return null; else return new Long(getMin());
		    case 2: if (getCount() == 0) return null; else return new Long(getMax());
		    case 3: if (getCount() == 0) return null; else return new Double(getAvg());
			case 4: if (getCount() == 0) return null; else return new Double(getStdDev());
		    case 5: if (getCount() == 0) return null; else return new Long(getFirst());
		    case 6: if (getCount() == 0) return null; else return new Long(getLast());
			case 7: if (getCount() == 0) return null; else return new Long(getTotal());
		    default : if (getCount() == 0) return null; 
		    	else return new Double(new Double(classCounts[index-NUM_STATIC_ITEMS]).doubleValue()/getCount());
	    }
    }
    public long getFirst() {
	    return first;
    }
	public long getLast() {
		return last;
	}
    public long getMax()
    {
        return max;
    }
    public long getMin()
    {
        return min;
    }

	public String getName() {
		return name;
	}
    public double getStdDev() {
    	
    	return Math.sqrt(getVariance());
    }
    public long getTotal()
    {
        return total;
    }
    public long getTotalSquare(){
    	return totalSquare;
    }
    public double getVariance(){
    	double result;
    	if (count>1) {
    		result=(totalSquare-((total*total)/count))/(count-1);

    	}
    	else result=Double.NaN;
    	return result;
    }
}
