/*
 * $Log: StatisticsKeeper.java,v $
 * Revision 1.10  2006-09-07 08:39:12  europe\L190409
 * corrected version String
 *
 * Revision 1.9  2006/09/07 08:38:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added dumpToXml()
 *
 * Revision 1.8  2005/03/10 09:52:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked percentile estimation
 *
 * Revision 1.7  2005/02/17 09:55:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed percentile estimator to basic
 *
 * Revision 1.6  2005/02/02 16:37:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modular percentile estimation
 *
 * Revision 1.5  2005/01/13 09:05:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added percentile estimations
 *
 */
package nl.nn.adapterframework.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Keeps statistics (min, max, count etc).
 * <p>Creation date: (19-02-2003 11:34:14)</p>
 * @version Id
 * @author Johan Verrips / Gerrit van Brakel
 */
public class StatisticsKeeper {
	public static final String version="$RCSfile: StatisticsKeeper.java,v $ $Revision: 1.10 $ $Date: 2006-09-07 08:39:12 $";
	
	private static final boolean calculatePercentiles=true;
	
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

	public static final String percentileConfigKey="Statistics.percentiles";
	public static final String DEFAULT_P_LIST="50,90,95,98";

	protected PercentileEstimator pest;	

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

		if (calculatePercentiles) {
//			pest = new PercentileEstimatorBase(percentileConfigKey,DEFAULT_P_LIST,1000);
			pest = new PercentileEstimatorRanked(percentileConfigKey,DEFAULT_P_LIST,100);
		}
	}
/*	
	public StatisticsKeeper(StatisticsKeeper stat) {
		name = stat.name;
	    min = stat.min;
	    max = stat.max;
	    count = stat.count;
	    total = stat.total;
	    totalSquare = stat.totalSquare;
	
	    classBoundaries = stat.getClassBoundaries();
	    classCounts = new long[classBoundaries.length];

		pest = stat.pest;
	}
*/	
	public void addValue(long value) {
		if (count==0) { 
			first=value;
		}
	    ++count;
		if (calculatePercentiles) {
			pest.addValue(value,count,min,max);
		}
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
	


    public double getAvg()
    {
        if (count == 0)
        {
            return 0;
        }
        return (total / (double)count);
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
		if (calculatePercentiles) {
	        return NUM_STATIC_ITEMS+classBoundaries.length+pest.getNumPercentiles();
		}
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
		    default : if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
				return "< "+classBoundaries[index-NUM_STATIC_ITEMS]+"ms";
		    }
		    if (calculatePercentiles) {
				return "p"+pest.getPercentage(index-NUM_STATIC_ITEMS-classBoundaries.length);
		    }
			return null;
	    }
    }
    
	public int getItemIndex(String name) {
		int top=NUM_STATIC_ITEMS+classBoundaries.length;
		if (calculatePercentiles) {
			top+=pest.getNumPercentiles();
		}
			
		for (int i=0; i<top; i++) {
			if (getItemName(i).equals(name)) {
				return i;
			}
		}
		return -1;
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
    public Object getItemValue(int index) {
	    switch (index) {
		    case 0: return new Long(getCount());
		    case 1: if (getCount() == 0) return null; else return new Long(getMin());
		    case 2: if (getCount() == 0) return null; else return new Long(getMax());
		    case 3: if (getCount() == 0) return null; else return new Double(getAvg());
			case 4: if (getCount() == 0) return null; else return new Double(getStdDev());
		    case 5: if (getCount() == 0) return null; else return new Long(getFirst());
		    case 6: if (getCount() == 0) return null; else return new Long(getLast());
			case 7: if (getCount() == 0) return null; else return new Long(getTotal());
		    default : if ((getCount() == 0)) return null;
		    	if (index<0) {
					throw new ArrayIndexOutOfBoundsException("StatisticsKeeper.getItemValue() item index negative: "+index);
		    	}
				if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
					return new Double(new Double(classCounts[index-NUM_STATIC_ITEMS]).doubleValue()/getCount());
				}
				if (calculatePercentiles) {
					return new Double(pest.getPercentileEstimate(index-NUM_STATIC_ITEMS-classBoundaries.length,getCount(),getMin(),getMax()));
				}
				throw new ArrayIndexOutOfBoundsException("StatisticsKeeper.getItemValue() item index too high: "+index);
	    }
    }

	public String getItemValueFormated(int index) {
		Object item = getItemValue(index);
		if (item==null) {
			return "-";
		} else {
			switch (getItemType(index)) {
				case StatisticsKeeper.ITEM_TYPE_INTEGER: 
					return ""+ (Long)item;
				case StatisticsKeeper.ITEM_TYPE_TIME: 
					DecimalFormat df=new DecimalFormat(DateUtils.FORMAT_MILLISECONDS);
					return df.format(item);
				case StatisticsKeeper.ITEM_TYPE_FRACTION:
					DecimalFormat pf=new DecimalFormat("##0.0");
					return ""+pf.format(((Double)item).doubleValue()*100);
				default:
					return item.toString();
			}
		}
	}
    
    public XmlBuilder dumpToXml() {
		XmlBuilder result = new XmlBuilder("StatisticsKeeper");
		XmlBuilder items = new XmlBuilder("items");
		result.addSubElement(items);
		for (int i=0;i<getItemCount();i++) {
			XmlBuilder item = new XmlBuilder("item");
			items.addSubElement(item);
			item.addAttribute("index",""+i);
			item.addAttribute("name",XmlUtils.encodeChars(getItemName(i)));
			item.addAttribute("type",""+getItemType(i));
			item.addAttribute("value",getItemValueFormated(i));
		}
		XmlBuilder item = new XmlBuilder("item");
		items.addSubElement(item);
		item.addAttribute("index","-1");
		item.addAttribute("name","sumofsquares");
		item.addAttribute("value",""+totalSquare);

		XmlBuilder samples = new XmlBuilder("samples");
		result.addSubElement(samples);
		for (int i=0;i<pest.getSampleCount(getCount(),getMin(),getMax());i++) {
			XmlBuilder sample = pest.getSample(i,getCount(),getMin(),getMax());
			samples.addSubElement(sample);
		}
    	return result;
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
