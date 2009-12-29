/*
 * $Log: StatisticsKeeperXmlBuilder.java,v $
 * Revision 1.1  2009-12-29 14:25:18  L190409
 * moved statistics to separate package
 *
 * Revision 1.6  2009/08/26 15:44:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for separated adapter-only and detailed statistics
 *
 * Revision 1.5  2009/06/05 07:38:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for adapter level only statistics
 * added heapSize and totalMemory attributes
 *
 * Revision 1.4  2009/01/08 16:45:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added period indicator attributes to generated XML
 *
 * Revision 1.3  2008/09/04 12:19:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect interval statistics
 *
 * Revision 1.2  2008/09/01 15:37:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added generic summary information
 *
 * Revision 1.1  2008/06/03 15:57:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.statistics;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.log4j.Logger;

/**
 * Make XML of all statistics info.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class StatisticsKeeperXmlBuilder implements StatisticsKeeperIterationHandler {
	protected Logger log = LogUtil.getLogger(this);

	private DecimalFormat df=new DecimalFormat(ItemList.ITEM_FORMAT_TIME);
	private DecimalFormat pf=new DecimalFormat(ItemList.ITEM_FORMAT_PERC);

	private DateFormat dtf=new SimpleDateFormat(DateUtils.FORMAT_FULL_GENERIC);


	private static final String ROOT_ELEMENT="statisticsCollection";
	private static final String GROUP_ELEMENT="statgroup";
	private static final String STATKEEPER_ELEMENT="stat";
	private static final String STATKEEPER_SUMMARY_ELEMENT="cumulative";
	private static final String STATKEEPER_INTERVAL_ELEMENT="interval";
	private static final String STATISTICS_XML_VERSION="1";


	public StatisticsKeeperXmlBuilder() {
		super();
	}

	public XmlBuilder getXml(Object data) {
		return (XmlBuilder)data; 
	}

	public void configure() throws ConfigurationException {
	}	

	public Object start(Date now, Date mainMark, Date detailMark) {
		log.debug("StatisticsKeeperXmlBuilder.start()");
		
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		
		XmlBuilder root = new XmlBuilder(ROOT_ELEMENT);
		root.addAttribute("version",STATISTICS_XML_VERSION);
		root.addAttribute("heapSize", Long.toString (totalMem-freeMem) );
		root.addAttribute("totalMemory", Long.toString(totalMem) );
		root.addAttribute("timestamp",DateUtils.format(now,DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("intervalStart",DateUtils.format(mainMark,DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("host",Misc.getHostname());
		root.addAttribute("instance",AppConstants.getInstance().getProperty("instance.name"));
		addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_HOUR,PERIOD_FORMAT_DATEHOUR},PERIOD_ALLOWED_LENGTH_HOUR,"",mainMark);
		addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_DAY,PERIOD_FORMAT_DATE,PERIOD_FORMAT_WEEKDAY},PERIOD_ALLOWED_LENGTH_DAY,"",mainMark);
		addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_WEEK,PERIOD_FORMAT_YEARWEEK},PERIOD_ALLOWED_LENGTH_WEEK,"",mainMark);
		addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_MONTH,PERIOD_FORMAT_YEARMONTH},PERIOD_ALLOWED_LENGTH_MONTH,"",mainMark);
		addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_YEAR},PERIOD_ALLOWED_LENGTH_YEAR,"",mainMark);
		if (detailMark!=null) {
			root.addAttribute("intervalStartDetail",DateUtils.format(detailMark,DateUtils.FORMAT_GENERICDATETIME));
			addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_HOUR,PERIOD_FORMAT_DATEHOUR},PERIOD_ALLOWED_LENGTH_HOUR,"Detail",detailMark);
			addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_DAY,PERIOD_FORMAT_DATE,PERIOD_FORMAT_WEEKDAY},PERIOD_ALLOWED_LENGTH_DAY,"Detail",detailMark);
			addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_WEEK,PERIOD_FORMAT_YEARWEEK},PERIOD_ALLOWED_LENGTH_WEEK,"Detail",detailMark);
			addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_MONTH,PERIOD_FORMAT_YEARMONTH},PERIOD_ALLOWED_LENGTH_MONTH,"Detail",detailMark);
			addPeriodIndicator(root,now,new String[][]{PERIOD_FORMAT_YEAR},PERIOD_ALLOWED_LENGTH_YEAR,"Detail",detailMark);
		}
		return root;
	}
	
	public void end(Object data) {
		log.debug("StatisticsKeeperXmlBuilder.end()");
	}

	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
		XmlBuilder context=(XmlBuilder)data;
		XmlBuilder item = statisticsKeeperToXmlBuilder(sk,STATKEEPER_ELEMENT);
		if (item!=null) {
			context.addSubElement(item);
		}
	}

	public void handleScalar(Object data, String scalarName, long value){
		handleScalar(data,scalarName,""+value);
	}

	public void handleScalar(Object data, String scalarName, Date value){
		if (value!=null) {
			handleScalar(data,scalarName,dtf.format(value));
		} 
	}


	public void handleScalar(Object data, String scalarName, String value){
		XmlBuilder context=(XmlBuilder)data;
		addNumber(context,scalarName,value);
	}

	public Object openGroup(Object parentData, String name, String type) {
		XmlBuilder context=(XmlBuilder)parentData;
		XmlBuilder group= new XmlBuilder(GROUP_ELEMENT);
		group.addAttribute("name",name);
		group.addAttribute("type",type);
		context.addSubElement(group);
		return group;
	}

	public void closeGroup(Object data) {
	}

	private void addPeriodIndicator(XmlBuilder xml, Date now, String[][] periods, long allowedLength, String suffix, Date mark) {
		long intervalStart=mark.getTime(); 
		long intervalEnd=now.getTime();
		if ((intervalEnd-intervalStart)<=allowedLength) {
			Date midterm=new Date((intervalEnd>>1)+(intervalStart>>1));
			for (int i=0; i<periods.length; i++) {
				String[] periodPair=periods[i];
				xml.addAttribute(periodPair[0]+suffix,DateUtils.format(midterm,periodPair[1]));
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
		if (sk==null) {
			return null;
		}
		String name = sk.getName();
		XmlBuilder container = new XmlBuilder(elementName);
		if (name!=null)
			container.addAttribute("name", name);
			
		XmlBuilder cumulativeStats = new XmlBuilder(STATKEEPER_SUMMARY_ELEMENT);
	
		for (int i=0; i<sk.getItemCount(); i++) {
			Object item = sk.getItemValue(i);
			if (item==null) {
				addNumber(cumulativeStats, sk.getItemName(i), "-");
			} else {
				switch (sk.getItemType(i)) {
					case StatisticsKeeper.ITEM_TYPE_INTEGER: 
						addNumber(cumulativeStats, sk.getItemName(i), ""+ (Long)item);
						break;
					case StatisticsKeeper.ITEM_TYPE_TIME: 
						addNumber(cumulativeStats, sk.getItemName(i), df.format(item));
						break;
					case StatisticsKeeper.ITEM_TYPE_FRACTION:
						addNumber(cumulativeStats, sk.getItemName(i), ""+pf.format(((Double)item).doubleValue()*100)+ "%");
						break;
				}
			}
		}
		container.addSubElement(cumulativeStats);
		XmlBuilder intervalStats = new XmlBuilder(STATKEEPER_INTERVAL_ELEMENT);
	
		for (int i=0; i<sk.getIntervalItemCount(); i++) {
			Object item = sk.getIntervalItemValue(i);
			if (item==null) {
				addNumber(intervalStats, sk.getIntervalItemName(i), "-");
			} else {
				switch (sk.getIntervalItemType(i)) {
					case StatisticsKeeper.ITEM_TYPE_INTEGER: 
						addNumber(intervalStats, sk.getIntervalItemName(i), ""+ (Long)item);
						break;
					case StatisticsKeeper.ITEM_TYPE_TIME: 
						addNumber(intervalStats, sk.getIntervalItemName(i), df.format(item));
						break;
					case StatisticsKeeper.ITEM_TYPE_FRACTION:
						addNumber(intervalStats, sk.getIntervalItemName(i), ""+pf.format(((Double)item).doubleValue()*100)+ "%");
						break;
				}
			}
		}
		container.addSubElement(intervalStats);
		return container;
	}


}
