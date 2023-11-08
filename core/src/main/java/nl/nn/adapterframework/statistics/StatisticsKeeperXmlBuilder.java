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

import java.text.DecimalFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Make XML of all statistics info.
 *
 * @author Gerrit van Brakel
 * @since 4.9
 */
public class StatisticsKeeperXmlBuilder implements StatisticsKeeperIterationHandler<XmlBuilder> {
	protected Logger log = LogUtil.getLogger(this);

	private DecimalFormat df = new DecimalFormat(ItemList.ITEM_FORMAT_TIME);
	private DecimalFormat pf = new DecimalFormat(ItemList.ITEM_FORMAT_PERC);
	private static final String ROOT_ELEMENT = "statisticsCollection";
	private static final String GROUP_ELEMENT = "statgroup";
	private static final String STATKEEPER_ELEMENT = "stat";
	private static final String STATKEEPER_SUMMARY_ELEMENT = "cumulative";
	private static final String STATKEEPER_INTERVAL_ELEMENT = "interval";
	private static final String STATISTICS_XML_VERSION = "1";


	public StatisticsKeeperXmlBuilder() {
		super();
	}

	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public XmlBuilder start(Date now, Date mainMark, Date detailMark) {
		log.debug("StatisticsKeeperXmlBuilder.start()");

		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();

		XmlBuilder root = new XmlBuilder(ROOT_ELEMENT);
		root.addAttribute("version", STATISTICS_XML_VERSION);
		root.addAttribute("heapSize", Long.toString(totalMem - freeMem));
		root.addAttribute("totalMemory", Long.toString(totalMem));
		root.addAttribute("timestamp", DateUtils.format(now, DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("intervalStart", DateUtils.format(mainMark, DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("host", Misc.getHostname());
		root.addAttribute("instance", AppConstants.getInstance().getProperty("instance.name"));
		addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_HOUR, PERIOD_FORMAT_DATEHOUR}, PERIOD_ALLOWED_LENGTH_HOUR, "", mainMark);
		addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_DAY, PERIOD_FORMAT_DATE, PERIOD_FORMAT_WEEKDAY}, PERIOD_ALLOWED_LENGTH_DAY, "", mainMark);
		addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_WEEK, PERIOD_FORMAT_YEARWEEK}, PERIOD_ALLOWED_LENGTH_WEEK, "", mainMark);
		addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_MONTH, PERIOD_FORMAT_YEARMONTH}, PERIOD_ALLOWED_LENGTH_MONTH, "", mainMark);
		addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_YEAR}, PERIOD_ALLOWED_LENGTH_YEAR, "", mainMark);
		if (detailMark != null) {
			root.addAttribute("intervalStartDetail", DateUtils.format(detailMark, DateUtils.FORMAT_GENERICDATETIME));
			addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_HOUR, PERIOD_FORMAT_DATEHOUR}, PERIOD_ALLOWED_LENGTH_HOUR, "Detail", detailMark);
			addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_DAY, PERIOD_FORMAT_DATE, PERIOD_FORMAT_WEEKDAY}, PERIOD_ALLOWED_LENGTH_DAY, "Detail", detailMark);
			addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_WEEK, PERIOD_FORMAT_YEARWEEK}, PERIOD_ALLOWED_LENGTH_WEEK, "Detail", detailMark);
			addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_MONTH, PERIOD_FORMAT_YEARMONTH}, PERIOD_ALLOWED_LENGTH_MONTH, "Detail", detailMark);
			addPeriodIndicator(root, now, new String[][]{PERIOD_FORMAT_YEAR}, PERIOD_ALLOWED_LENGTH_YEAR, "Detail", detailMark);
		}
		return root;
	}

	@Override
	public void end(XmlBuilder data) {
		log.debug("StatisticsKeeperXmlBuilder.end()");
	}

	@Override
	public void handleStatisticsKeeper(XmlBuilder context, StatisticsKeeper sk) {
		XmlBuilder item = statisticsKeeperToXmlBuilder(sk, STATKEEPER_ELEMENT);
		if (item != null) {
			context.addSubElement(item);
		}
	}

	@Override
	public void handleScalar(XmlBuilder context, String name, ScalarMetricBase<?> meter) throws SenderException {
		handleScalar(context, name, meter.getValue());
	}

	@Override
	public void handleScalar(XmlBuilder data, String scalarName, long value) {
		handleScalar(data, scalarName, "" + value);
	}

	@Override
	public void handleScalar(XmlBuilder data, String scalarName, Date value) {
		if (value != null) {
			handleScalar(data, scalarName, DateUtils.format(value));
		}
	}


	private void handleScalar(XmlBuilder context, String scalarName, String value) {
		addNumber(context, scalarName, value);
	}

	@Override
	public XmlBuilder openGroup(XmlBuilder context, String name, String type) {
		XmlBuilder group = new XmlBuilder(GROUP_ELEMENT);
		group.addAttribute("name", name);
		group.addAttribute("type", type);
		context.addSubElement(group);
		return group;
	}

	@Override
	public void closeGroup(XmlBuilder data) {
	}

	private void addPeriodIndicator(XmlBuilder xml, Date now, String[][] periods, long allowedLength, String suffix, Date mark) {
		long intervalStart = mark.getTime();
		long intervalEnd = now.getTime();
		if ((intervalEnd - intervalStart) <= allowedLength) {
			Date midterm = new Date((intervalEnd >> 1) + (intervalStart >> 1));
			for (int i = 0; i < periods.length; i++) {
				String[] periodPair = periods[i];
				xml.addAttribute(periodPair[0] + suffix, DateUtils.format(midterm, periodPair[1]));
			}
		}
	}

	private void addNumber(XmlBuilder xml, String name, String value) {
		XmlBuilder item = new XmlBuilder("item");

		item.addAttribute("name", name);
		item.addAttribute("value", value);
		xml.addSubElement(item);
	}

	public XmlBuilder statisticsKeeperToXmlBuilder(StatisticsKeeper sk, String elementName) {
		if (sk == null) {
			return null;
		}
		String name = sk.getName();
		XmlBuilder container = new XmlBuilder(elementName);
		if (name != null)
			container.addAttribute("name", name);

		XmlBuilder cumulativeStats = new XmlBuilder(STATKEEPER_SUMMARY_ELEMENT);

		for (int i = 0; i < sk.getItemCount(); i++) {
			Object item = sk.getItemValue(i);
			if (item == null) {
				addNumber(cumulativeStats, sk.getItemName(i), "-");
			} else {
				switch (sk.getItemType(i)) {
					case INTEGER:
						addNumber(cumulativeStats, sk.getItemName(i), "" + (Long) item);
						break;
					case TIME:
						addNumber(cumulativeStats, sk.getItemName(i), df.format(item));
						break;
					case FRACTION:
						addNumber(cumulativeStats, sk.getItemName(i), "" + pf.format(((Double) item).doubleValue() * 100) + "%");
						break;
				}
			}
		}
		container.addSubElement(cumulativeStats);
		XmlBuilder intervalStats = new XmlBuilder(STATKEEPER_INTERVAL_ELEMENT);

		for (int i = 0; i < sk.getIntervalItemCount(); i++) {
			Object item = sk.getIntervalItemValue(i);
			if (item == null) {
				addNumber(intervalStats, sk.getIntervalItemName(i), "-");
			} else {
				switch (sk.getIntervalItemType(i)) {
					case INTEGER:
						addNumber(intervalStats, sk.getIntervalItemName(i), "" + (Long) item);
						break;
					case TIME:
						addNumber(intervalStats, sk.getIntervalItemName(i), df.format(item));
						break;
					case FRACTION:
						addNumber(intervalStats, sk.getIntervalItemName(i), "" + pf.format(((Double) item).doubleValue() * 100) + "%");
						break;
				}
			}
		}
		container.addSubElement(intervalStats);
		return container;
	}


}
