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
package org.frankframework.statistics;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.logging.log4j.Logger;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * Keeps statistics (min, max, count etc).
 *
 * @author Johan Verrips / Gerrit van Brakel
 */
public class StatisticsKeeper<B, S> {
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
	}

	protected StatisticsKeeper(String name, B basics, String boundaryConfigKey, String defaultBoundaryList) {
		super();
		AppConstants appConstants = AppConstants.getInstance();

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
	}

	public String getUnits() {
		return "ms";
	}

	public void addValue(long value) {
	}


	public long[] getClassBoundaries() {
		return classBoundaries;
	}
	public long[] getClassCounts() {
		return classCounts;
	}
}
