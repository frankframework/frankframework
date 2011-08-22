/*
 * $Log: StatisticsKeeper.java,v $
 * Revision 1.3  2011-08-22 14:31:32  L190409
 * support for size statistics
 *
 * Revision 1.2  2011/05/23 13:41:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'total' fields to 'sum'
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics to separate package
 *
 * Revision 1.14  2009/06/05 07:36:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
package nl.nn.adapterframework.statistics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import nl.nn.adapterframework.statistics.percentiles.PercentileEstimator;
import nl.nn.adapterframework.statistics.percentiles.PercentileEstimatorRanked;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Keeps statistics (min, max, count etc).
 * 
 * @author Johan Verrips / Gerrit van Brakel
 * @version Id
 */
public class StatisticsKeeper implements ItemList {
	public static final String version="$RCSfile: StatisticsKeeper.java,v $ $Revision: 1.3 $ $Date: 2011-08-22 14:31:32 $";

	private static final boolean calculatePercentiles=true;
	
	private String name = null;
	private long first=Long.MIN_VALUE;
    private long last=0;
    private Basics cumulative;
	private Basics mark;
    private long classBoundaries[];
    private long classCounts[];
    
    

 
 	public static final int NUM_STATIC_ITEMS=8;   
	public static final int NUM_INTERVAL_ITEMS=6;   

   	// key that is looked up to retrieve texts to be signalled
	private static final String statConfigKey="Statistics.boundaries";
    public static final String DEFAULT_BOUNDARY_LIST="100,500,1000,5000";
    
	public static final String ITEM_NAME_FIRST="first";
	public static final String ITEM_NAME_LAST="last";

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
		this(name, Basics.class, statConfigKey, DEFAULT_BOUNDARY_LIST);
	}

	protected StatisticsKeeper(String name, Class basicsClass, String boundaryConfigKey, String defaultBoundaryList) {
	    super();
	    this.name = name;
	    try {
			cumulative=(Basics)basicsClass.newInstance();
			mark=(Basics)basicsClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	    
	    List classBoundariesBuffer = new ArrayList();
	
	    StringTokenizer tok = AppConstants.getInstance().getTokenizer(boundaryConfigKey,defaultBoundaryList);
	
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
	
	public String getUnits() {
		return "ms";
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
		if (first==Long.MIN_VALUE) { 
			first=value;
		}
		last = value;
		long curMin=cumulative.getMin();
		long curMax=cumulative.getMax();
		cumulative.addValue(value);
		mark.checkMinMax(value);
		if (calculatePercentiles) {
			pest.addValue(value,cumulative.getCount(),curMin,curMax);
		}
	
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
    	if (index<Basics.NUM_BASIC_ITEMS) {
    		return cumulative.getItemName(index);
    	}
	    switch (index) {
			case 6: return ITEM_NAME_FIRST;
			case 7: return ITEM_NAME_LAST;
		    default : if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) { 
				return "< "+classBoundaries[index-NUM_STATIC_ITEMS]+getUnits();
		    }
		    if (calculatePercentiles) {
				return "p"+pest.getPercentage(index-NUM_STATIC_ITEMS-classBoundaries.length);
		    }
			return null;
	    }
    }
	public String getIntervalItemName(int index) {
		switch (index) {
			case 0: return ITEM_NAME_COUNT;
			case 1: return ITEM_NAME_MIN;
			case 2: return ITEM_NAME_MAX;
			case 3: return ITEM_NAME_AVERAGE;
			case 4: return ITEM_NAME_SUM;
			case 5: return ITEM_NAME_SUMSQ;
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
		if (index<Basics.NUM_BASIC_ITEMS) {
			return cumulative.getItemType(index);
		}
	    switch (index) {
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
		if (index<Basics.NUM_BASIC_ITEMS) {
			return cumulative.getItemValue(index);
		}
	    switch (index) {
		    case 6: if (getCount() == 0) return null; else return new Long(getFirst());
		    case 7: if (getCount() == 0) return null; else return new Long(getLast());
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
			case 0: return new Long(cumulative.getIntervalCount(mark));
			case 1: if (cumulative.getCount() == mark.getCount()) return null; else return new Long(mark.getMin());
			case 2: if (cumulative.getCount() == mark.getCount()) return null; else return new Long(mark.getMax());
			case 3: if (cumulative.getCount() == mark.getCount()) return null; else return new Double(cumulative.getIntervalAverage(mark));
			case 4: return new Long(cumulative.getIntervalSum(mark));
			case 5: return new Long(cumulative.getIntervalSumOfSquares(mark));
			default : return null;
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
			item.addAttribute("value",ItemUtil.getItemValueFormated(this,i));
		}
		XmlBuilder item = new XmlBuilder("item");
		items.addSubElement(item);
		item.addAttribute("index","-1");
		item.addAttribute("name","sumofsquares");
		item.addAttribute("value",""+cumulative.getSumOfSquares());

		XmlBuilder samples = new XmlBuilder("samples");
		result.addSubElement(samples);
		for (int i=0;i<pest.getSampleCount(getCount(),getMin(),getMax());i++) {
			XmlBuilder sample = pest.getSample(i,getCount(),getMin(),getMax());
			samples.addSubElement(sample);
		}
    	return result;
    }
 

	public XmlBuilder toXml(String elementName, boolean deep, DecimalFormat timeFormat, DecimalFormat percentageFormat) {
		if (deep) {
			 return dumpToXml();
		}
		return ItemUtil.toXml(this, elementName, getName(), timeFormat, percentageFormat);
	}

   
    
	public long getCount() {
		return cumulative.getCount();
	}
	
	public double getAvg() {
		return cumulative.getAverage();
	}
   
    
    public long getFirst() {
	    return first;
    }
	public long getLast() {
		return last;
	}
    public long getMax() {
        return cumulative.getMax();
    }
    public long getMin() {
        return cumulative.getMin();
    }

	public String getName() {
		return name;
	}
    public double getStdDev() {
    	return Math.sqrt(getVariance());
    }
    public long getTotal() {
        return cumulative.getSum();
    }
    public long getTotalSquare(){
    	return cumulative.getSumOfSquares();
    }
    public double getVariance() {
    	return cumulative.getVariance();
    }
}
