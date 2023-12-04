/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Logger;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import nl.nn.adapterframework.statistics.HasStatistics.Action;
import nl.nn.adapterframework.statistics.percentiles.PercentileEstimator;
import nl.nn.adapterframework.statistics.percentiles.PercentileEstimatorRanked;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlEncodingUtils;

/**
 * Keeps statistics (min, max, count etc).
 *
 * @author Johan Verrips / Gerrit van Brakel
 */
public class StatisticsKeeper<B extends IBasics<S>, S> implements ItemList {
	protected static Logger log = LogUtil.getLogger(StatisticsKeeper.class);

	private String name = null;
	private long first = Long.MIN_VALUE;
	private long last = 0;
	private B cumulative;
	private S mark;
	private long[] classBoundaries;
	private long[] classCounts;

	public static final String BASICS_KEY="Statistics.basics.class";

	public static final int NUM_STATIC_ITEMS=8;
	public static final int NUM_INTERVAL_ITEMS=6;

	// key that is looked up to retrieve texts to be signalled
	private static final String statConfigKey = "Statistics.boundaries";
	public static final String DEFAULT_BOUNDARY_LIST = "100,500,1000,5000";

	public static final String ITEM_NAME_FIRST="first";
	public static final String ITEM_NAME_LAST="last";

	public static final String percentileConfigKey="Statistics.percentiles";
	public static final String DEFAULT_P_LIST="50,90,95,98";

	public static final String PERCENTILE_PUBLISH_KEY="Statistics.percentiles.publish";
	public static final String HISTOGRAM_PUBLISH_KEY="Statistics.histograms.publish";
	public static final String PERCENTILES_INTERNAL_KEY="Statistics.percentiles.internal";
	public static final String PERCENTILE_PRECISION_KEY="Statistics.percentiles.precision";

	private boolean publishPercentiles;
	private boolean publishHistograms;
	private boolean calculatePercentiles;
	private int percentilePrecision;

	protected PercentileEstimator pest;

	private DistributionSummary distributionSummary;

	private static List<String> labels;
	private static List<String> types;

	/**
	 * Constructor for StatisticsKeeper.
	 *
	 * Initializes statistics. Reads values for class boundaries from AppConstants
	 *
	 * @see AppConstants
	 */
	public StatisticsKeeper(String name) {
		this(name, null, statConfigKey, DEFAULT_BOUNDARY_LIST);
	}

	protected StatisticsKeeper(String name, B basics) {
		this(name, basics, statConfigKey, DEFAULT_BOUNDARY_LIST);
	}

	public void initMetrics(MeterRegistry registry, String name, Iterable<Tag> tags) {
		DistributionSummary.Builder builder = DistributionSummary
				.builder(name)
				.baseUnit(getUnits())
				.tags(tags)
				.tag("name", getName());
		double[] percentiles=null;
		if (publishPercentiles || publishHistograms) {
			builder.percentilePrecision(percentilePrecision);

			if (pest!=null && pest.getNumPercentiles()>0) {
				percentiles = new double[pest.getNumPercentiles()];
				for (int i=0;i<pest.getNumPercentiles();i++) {
					percentiles[i]=((double)pest.getPercentage(i))/100.0;
				}
				builder.publishPercentiles(percentiles);
			}

			if (classBoundaries.length>0) {
				double[] serviceLevelObjectives = new double[classBoundaries.length];
				for (int i=0;i<classBoundaries.length;i++) {
					serviceLevelObjectives[i]=classBoundaries[i];
				}
				builder.serviceLevelObjectives(serviceLevelObjectives);
			}
			if (publishHistograms) {
				builder.publishPercentileHistogram();
			}
		}
		DistributionSummary distributionSummary = builder.register(registry);

		if (cumulative instanceof MicroMeterBasics) {
			((MicroMeterBasics)cumulative).setDistributionSummary(distributionSummary);
			mark=cumulative.takeSnapshot();
		} else {
			this.distributionSummary = distributionSummary;
		}
	}

	protected StatisticsKeeper(String name, B basics, String boundaryConfigKey, String defaultBoundaryList) {
		super();
		AppConstants appConstants = AppConstants.getInstance();

		if (basics==null) {
			String basicsClass = appConstants.getString(BASICS_KEY, Basics.class.getName());
			try {
				basics = (B)ClassUtils.newInstance(basicsClass);
			} catch (Exception e) {
				log.warn("Could not instantiate Basics class ["+basicsClass+"]", e);
				basics = (B)new Basics();
			}
		}

		StringTokenizer boundariesTokenizer = appConstants.getTokenizedProperty(boundaryConfigKey, defaultBoundaryList);

		publishPercentiles = appConstants.getBoolean(PERCENTILE_PUBLISH_KEY, false);
		publishHistograms = appConstants.getBoolean(HISTOGRAM_PUBLISH_KEY, false);
		calculatePercentiles = appConstants.getBoolean(PERCENTILES_INTERNAL_KEY, false);
		percentilePrecision = appConstants.getInt(PERCENTILE_PRECISION_KEY, 1);
		initialize(name, basics, boundariesTokenizer, publishPercentiles, publishHistograms, calculatePercentiles, percentilePrecision);
	}

	public StatisticsKeeper(String name, B basics, StringTokenizer boundariesTokenizer, boolean publishPercentiles, boolean publishHistograms, boolean calculatePercentiles, int percentilePrecision) {
		super();
		initialize(name, basics, boundariesTokenizer, publishPercentiles, publishHistograms, calculatePercentiles, percentilePrecision);
	}

	protected void initialize(String name, B basics, StringTokenizer boundariesTokenizer, boolean publishPercentiles, boolean publishHistograms, boolean calculatePercentiles, int percentilePrecision) {
		this.name = name;
		try {
			cumulative=basics;
			mark = cumulative.takeSnapshot();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		List classBoundariesBuffer = new ArrayList();


		while (boundariesTokenizer.hasMoreTokens()) {
			classBoundariesBuffer.add(new Long(Long.parseLong(boundariesTokenizer.nextToken())));
		}
		classBoundaries = new long[classBoundariesBuffer.size()];
		classCounts = new long[classBoundariesBuffer.size()];
		for (int i = 0; i < classBoundariesBuffer.size(); i++) {
			classBoundaries[i] = ((Long) classBoundariesBuffer.get(i)).longValue();
		}

		this.publishPercentiles = publishPercentiles;
		this.publishHistograms = publishHistograms;
		this.calculatePercentiles = publishPercentiles || publishHistograms || calculatePercentiles;
		this.percentilePrecision = percentilePrecision;

		if (this.calculatePercentiles) {
//			pest = new PercentileEstimatorBase(percentileConfigKey,DEFAULT_P_LIST,1000);
			pest = new PercentileEstimatorRanked(percentileConfigKey,DEFAULT_P_LIST,100);
		}
	}

	public String getUnits() {
		return "ms";
	}

	public void performAction(Action action) {
		if (action==Action.FULL || action==Action.SUMMARY) {
			return;
		}
		if (action==Action.MARK_FULL || action==Action.MARK_MAIN) {
			mark = cumulative.takeSnapshot();
		}
	}


	public void addValue(long value) {
		if (distributionSummary!=null) {
			distributionSummary.record(value);
		}
		if (first==Long.MIN_VALUE) {
			first=value;
		}
		last = value;
		long curMin=cumulative.getMin();
		long curMax=cumulative.getMax();
		cumulative.addValue(value);
		cumulative.updateIntervalMinMax(mark, value);
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

	@Override
	public int getItemCount() {
		if (calculatePercentiles) {
			return NUM_STATIC_ITEMS+classBoundaries.length+pest.getNumPercentiles();
		}
		return NUM_STATIC_ITEMS+classBoundaries.length;
	}
	public int getIntervalItemCount() {
		return NUM_INTERVAL_ITEMS;
	}

	@Override
	public String getItemName(int index) {
		if (index < IBasics.NUM_BASIC_ITEMS) {
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

	protected String getIntervalItemName(int index) {
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

	protected int getItemIndex(String name) {
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

	@Override
	public Type getItemType(int index) {
		if (index<IBasics.NUM_BASIC_ITEMS) {
			return cumulative.getItemType(index);
		}
		switch (index) {
			case 6: return Type.TIME;
			case 7: return Type.TIME;
			default :
				if ((index-NUM_STATIC_ITEMS) < classBoundaries.length) {
					return Type.FRACTION;
				}
				return Type.TIME;
		}
	}

	public Type getIntervalItemType(int index) {
		switch (index) {
			case 0: return Type.INTEGER;
			case 1: return Type.TIME;
			case 2: return Type.TIME;
			case 3: return Type.TIME;
			case 4: return Type.INTEGER;
			case 5: return Type.INTEGER;
			default : return Type.INTEGER;
		}
	}

	@Override
	public Object getItemValue(int index) {
		if (index<IBasics.NUM_BASIC_ITEMS) {
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
				throw new ArrayIndexOutOfBoundsException("StatisticsKeeper.getItemValue() item index too high: " + index);
		}
	}

	public Object getIntervalItemValue(int index) {
		switch (index) {
			case 0: return new Long(cumulative.getIntervalCount(mark));
			case 1: if (cumulative.getIntervalCount(mark) == 0) return null; return new Long(cumulative.getIntervalMin(mark));
			case 2: if (cumulative.getIntervalCount(mark) == 0) return null; return new Long(cumulative.getIntervalMax(mark));
			case 3: if (cumulative.getIntervalCount(mark) == 0) return null; return new Double(cumulative.getIntervalAverage(mark));
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
			item.addAttribute("name", XmlEncodingUtils.encodeChars(getItemName(i)));
			item.addAttribute("type", getItemType(i).name());
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

	public static List<String> getLabels() {
		if (labels == null) {
			List<String> newLabels = new ArrayList<>();
			newLabels.add("name");
			StatisticsKeeper tmp = new StatisticsKeeper("tmpStatKeeper");
			for (int i=0;i<tmp.getItemCount();i++) {
				newLabels.add(tmp.getItemName(i));
			}
			labels = newLabels;
		}
		return labels;
	}
	public static List<String> getTypes() {
		if (types == null) {
			List<String> newTypes = new ArrayList<>();
			newTypes.add("STRING");
			StatisticsKeeper tmp = new StatisticsKeeper("tmpStatKeeper");
			for (int i=0;i<tmp.getItemCount();i++) {
				newTypes.add(tmp.getItemType(i).name());
			}
			types = newTypes;
		}
		return types;
	}

	public Map<String, Object> asMap() {
		Map<String, Object> tmp = new LinkedHashMap<>();
		tmp.put("name", getName());
		for (int i=0; i< getItemCount(); i++) {
			Object item = getItemValue(i);
			String key = getItemName(i).replace("< ", "");
			if (item==null) {
				tmp.put(key, null);
			} else {
				switch (getItemType(i)) {
					case INTEGER:
						tmp.put(key, item);
						break;
					case TIME:
						if(item instanceof Long) {
							tmp.put(key, item);
						} else {
							Double val = (Double) item;
							if(val.isNaN() || val.isInfinite()) {
								tmp.put(key, null);
							} else {
								tmp.put(key, new BigDecimal(val).setScale(1, BigDecimal.ROUND_HALF_EVEN));
							}
						}
						break;
					case FRACTION:
						Double val = (Double) item;
						if(val.isNaN() || val.isInfinite()) {
							tmp.put(key, null);
						} else {
							tmp.put(key, new BigDecimal(((Double) item).doubleValue()*100).setScale(1,  BigDecimal.ROUND_HALF_EVEN));
						}
						break;
					default:
						throw new IllegalStateException("Unknown item type ["+getItemType(i)+"]");
				}
			}
		}
		return tmp;
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

	public long getTotalSquare() {
		return cumulative.getSumOfSquares();
	}

	public double getVariance() {
		return cumulative.getVariance();
	}
}
