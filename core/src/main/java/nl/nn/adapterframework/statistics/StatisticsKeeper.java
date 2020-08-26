/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
 */
public class StatisticsKeeper implements ItemList {

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
	
	    StringTokenizer tok = AppConstants.getInstance().getTokenizedProperty(boundaryConfigKey,defaultBoundaryList);
	
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
		return toXml(elementName, deep, timeFormat, percentageFormat, null);
	}

	public XmlBuilder toXml(String elementName, boolean deep, DecimalFormat timeFormat, DecimalFormat percentageFormat, DecimalFormat countFormat) {
		if (deep) {
			 return dumpToXml();
		}
		return ItemUtil.toXml(this, elementName, getName(), timeFormat, percentageFormat, countFormat);
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
