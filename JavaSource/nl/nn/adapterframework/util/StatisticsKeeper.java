/*
 * $Log: StatisticsKeeper.java,v $
 * Revision 1.5  2005-01-13 09:05:53  L190409
 * added percentile estimations
 *
 */
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
	public static final String version="$Id: StatisticsKeeper.java,v 1.5 2005-01-13 09:05:53 L190409 Exp $";
	
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
	private static final String statConfigKey="Statistics.boundaries";
 
 	public static final int NUM_STATIC_ITEMS=8;   
    public static final int ITEM_TYPE_INTEGER=1;
    public static final int ITEM_TYPE_TIME=2;
    public static final int ITEM_TYPE_FRACTION=3;
    public static final String DEFAULT_BOUNDARY_LIST="100,500,1000,5000";

	public static final String ewsaConfigKey="Statistics.percentiles";
	public static final String EWSA_DEFAULT_P_LIST="25,50,75,90,95";
	private static double EWSA_w=0.05;
	private int EWSA_p[];
	private double EWSA_S[];
	private double EWSA_f[];
	private int EWSA_S25=-1;
	private int EWSA_S75=-1;
	
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

		ArrayList ewsaPListBuffer = new ArrayList();
		tok = AppConstants.getInstance().getTokenizer(ewsaConfigKey,EWSA_DEFAULT_P_LIST);
	
		while (tok.hasMoreTokens()) {
			ewsaPListBuffer.add(new Integer(Integer.parseInt(tok.nextToken())));
		}
		EWSA_p = new int[ewsaPListBuffer.size()];
		EWSA_S = new double[ewsaPListBuffer.size()];
		EWSA_f = new double[ewsaPListBuffer.size()];
		for (int i = 0; i < ewsaPListBuffer.size(); i++) {
			int p = ((Integer) ewsaPListBuffer.get(i)).intValue();
			EWSA_p[i] = p;
			EWSA_S[i] = Double.NaN;
			if (p==25) {
				EWSA_S25=i;
			}
			if (p==75) {
				EWSA_S75=i;
			}
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
	    
		EWSA_p=stat.EWSA_p;
		EWSA_S=stat.EWSA_S;
		EWSA_f=stat.EWSA_f;
		EWSA_S25=stat.EWSA_S25;
		EWSA_S75=stat.EWSA_S75;
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
		update_EWSA(value);
	}
	
	
	
	public void update_EWSA(long value) {
		if (count>2) {
			double c=1/Math.sqrt(count);
			double rn=EWSA_S[EWSA_S75]-EWSA_S[EWSA_S25];
			double cn=rn*c;
			for (int i = 0; i < EWSA_p.length; i++) {
				EWSA_S[i] = EWSA_S[i] + (EWSA_w/EWSA_f[i]) * (0.01*EWSA_p[i]-((value <=EWSA_S[i]? 1.0 : 0)));
				if (EWSA_S[i] < min) {
					EWSA_S[i] = min;
				}
				if (EWSA_S[i] > max) {
					EWSA_S[i] = max;
				}
				EWSA_f[i] = (1-EWSA_w)*EWSA_f[i];
					 
				if (Math.abs(value-EWSA_S[i])<= cn) {
					EWSA_f[i] += EWSA_w/(2*cn);
				}
			}
		} else
			if (count==2) {
				// initial guesses
				float r0;
				if (min==max) {
					r0=1;
				} else {
					r0=(max-min)/2;
				}
				double c0 = r0 *((1/2)*(1+1/Math.sqrt(2)));
				double f0 = 1/c0;	
				for (int i = 0; i < EWSA_p.length; i++) {
					EWSA_S[i] = min + EWSA_p[i] * 0.01 * (max-min);
					EWSA_f[i] = f0;
				}
			} else {
				for (int i = 0; i < EWSA_p.length; i++) {
					EWSA_S[i] = value;
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
        return NUM_STATIC_ITEMS+classBoundaries.length+EWSA_p.length;
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
		    default : if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
				return "< "+classBoundaries[index-NUM_STATIC_ITEMS]+"ms";
		    }
			return "p"+EWSA_p[index-NUM_STATIC_ITEMS-classBoundaries.length];
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
			default :
				if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
					return ITEM_TYPE_FRACTION;
				}
				return ITEM_TYPE_TIME;
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
				if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
					return new Double(new Double(classCounts[index-NUM_STATIC_ITEMS]).doubleValue()/getCount());
				}
				return new Double(EWSA_S[index-NUM_STATIC_ITEMS-classBoundaries.length]);
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
