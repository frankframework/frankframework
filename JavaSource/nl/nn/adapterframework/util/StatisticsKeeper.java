/*
 * $Log: StatisticsKeeper.java,v $
 * Revision 1.14  2009-06-05 07:36:03  L190409
 * support for adapter level only statistics
 *
 * Revision 1.13  2008/09/04 13:26:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect interval statistics
 *
 * Revision 1.12  2008/08/27 16:25:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added clear()
 *
 * Revision 1.11  2007/10/08 13:35:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.10  2006/09/07 08:39:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.util.List;
import java.util.StringTokenizer;

/**
 * Keeps statistics (min, max, count etc).
 * 
 * @author Johan Verrips / Gerrit van Brakel
 * @version Id
 */
public class StatisticsKeeper {
	public static final String version="$RCSfile: StatisticsKeeper.java,v $ $Revision: 1.14 $ $Date: 2009-06-05 07:36:03 $";

	private static final boolean calculatePercentiles=true;
	
	private class Basics {
		long min = Integer.MAX_VALUE;
		long max = 0;
		long count = 0;
		long total = 0;
		long totalSquare=0;
		
		public void reset() {
			min = Integer.MAX_VALUE;
			max = 0;
			count = 0;
			total = 0;
			totalSquare=0;
		}

		public void mark(Basics other) {
			min = Integer.MAX_VALUE;
			max = 0;
			count = other.count;
			total = other.total;
			totalSquare=other.totalSquare;
		}
		
		
	}
	
	private String name = null;
	private long first=0;
    private long last=0;
    private Basics cumulative = new Basics();
	private Basics mark = new Basics();
    private long classBoundaries[];
    private long classCounts[];
    
    

   	// key that is looked up to retrieve texts to be signalled
	private static final String statConfigKey="Statistics.boundaries";
 
 	public static final int NUM_STATIC_ITEMS=8;   
	public static final int NUM_INTERVAL_ITEMS=6;   
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
	    List classBoundariesBuffer = new ArrayList();
	
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
	
	public void performAction(int action) {
		if (action==HasStatistics.STATISTICS_ACTION_FULL || action==HasStatistics.STATISTICS_ACTION_SUMMARY) {
			return;
		}
		if (action==HasStatistics.STATISTICS_ACTION_RESET) {
			clear();
		}
		if (action==HasStatistics.STATISTICS_ACTION_MARK_FULL || action==HasStatistics.STATISTICS_ACTION_MARK_MAIN) {
			mark.mark(cumulative);
		}
	}
	
	public void clear() {
		cumulative.reset();
		mark.reset();
		first=0;
		last=0;
		pest.clear();
	}
	
	public void addValue(long value) {
		if (cumulative.count==0) { 
			first=value;
		}
	    ++cumulative.count;
		if (calculatePercentiles) {
			pest.addValue(value,cumulative.count,cumulative.min,cumulative.max);
		}
		cumulative.total += value;
		if (value > mark.max) {
			mark.max = value;
		    if (value > cumulative.max) {
				cumulative.max = value;
		    }
	    }
		if (value < mark.min) {
			mark.min = value;
		    if (value < cumulative.min) {
				cumulative.min = value;
		    }
	    }
		cumulative.totalSquare += value * value;
	    last = value;
	
	    for (int i = 0; i < classBoundaries.length; i++) {
	        if (value < classBoundaries[i]) {
	            classCounts[i]++;
	        }
	    }
	}
	


	public long[] getClassBoundaries() {
		return classBoundaries;
	}
	public long[] getClassCounts() {
		return classCounts;
	}

    public int getItemCount() {
		if (calculatePercentiles) {
	        return NUM_STATIC_ITEMS+classBoundaries.length+pest.getNumPercentiles();
		}
		return NUM_STATIC_ITEMS+classBoundaries.length;
    }
	public int getIntervalItemCount() {
		return NUM_INTERVAL_ITEMS;
	}
	
    public String getItemName(int index) {
	    switch (index) {
		    case 0: return "count";
		    case 1: return "min";
		    case 2: return "max";
		    case 3: return "avg";
			case 4: return "stdDev";
			case 5: return "first";
			case 6: return "last";
		    case 7: return "sum";
		    default : if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
				return "< "+classBoundaries[index-NUM_STATIC_ITEMS]+"ms";
		    }
		    if (calculatePercentiles) {
				return "p"+pest.getPercentage(index-NUM_STATIC_ITEMS-classBoundaries.length);
		    }
			return null;
	    }
    }
	public String getIntervalItemName(int index) {
		switch (index) {
			case 0: return "count";
			case 1: return "min";
			case 2: return "max";
			case 3: return "avg";
			case 4: return "sum";
			case 5: return "sumsq";
			default: return null;
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

    
    public int getItemType(int index) {
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
	public int getIntervalItemType(int index) {
		switch (index) {
			case 0: return ITEM_TYPE_INTEGER;
			case 1: return ITEM_TYPE_TIME;
			case 2: return ITEM_TYPE_TIME;
			case 3: return ITEM_TYPE_TIME;
			case 4: return ITEM_TYPE_INTEGER;
			case 5: return ITEM_TYPE_INTEGER;
			default : return ITEM_TYPE_INTEGER;
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
	public Object getIntervalItemValue(int index) {
		switch (index) {
			case 0: return new Long(cumulative.count-mark.count);
			case 1: if (cumulative.count == mark.count) return null; else return new Long(mark.min);
			case 2: if (cumulative.count == mark.count) return null; else return new Long(mark.max);
			case 3: if (cumulative.count == mark.count) return null; else return new Double((cumulative.total-mark.total)/(double)(cumulative.count-mark.count));
			case 4: return new Long(cumulative.total-mark.total);
			case 5: return new Long(cumulative.totalSquare-mark.totalSquare);
			default : return null;
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
		item.addAttribute("value",""+cumulative.totalSquare);

		XmlBuilder samples = new XmlBuilder("samples");
		result.addSubElement(samples);
		for (int i=0;i<pest.getSampleCount(getCount(),getMin(),getMax());i++) {
			XmlBuilder sample = pest.getSample(i,getCount(),getMin(),getMax());
			samples.addSubElement(sample);
		}
    	return result;
    }
 
   
    
	public long getCount() {
		return cumulative.count;
	}
	
	public double getAvg() {
		if (cumulative.count == 0) {
			return 0;
		}
		return (cumulative.total / (double)cumulative.count);
	}
   
    
    public long getFirst() {
	    return first;
    }
	public long getLast() {
		return last;
	}
    public long getMax() {
        return cumulative.max;
    }
    public long getMin() {
        return cumulative.min;
    }

	public String getName() {
		return name;
	}
    public double getStdDev() {
    	return Math.sqrt(getVariance());
    }
    public long getTotal() {
        return cumulative.total;
    }
    public long getTotalSquare(){
    	return cumulative.totalSquare;
    }
    public double getVariance() {
    	double result;
    	if (cumulative.count>1) {
    		result=(cumulative.totalSquare-((cumulative.total*cumulative.total)/cumulative.count))/(cumulative.count-1);

    	}
    	else result=Double.NaN;
    	return result;
    }
}
