/*
 * $Log: StatisticsKeeperXmlBuilder.java,v $
 * Revision 1.4  2009-01-08 16:45:32  L190409
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
package nl.nn.adapterframework.util;

import java.text.DecimalFormat;
import java.util.Date;

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

	private DecimalFormat df=new DecimalFormat(DateUtils.FORMAT_MILLISECONDS);
	private DecimalFormat pf=new DecimalFormat("##0.0");

	private static final String ROOT_ELEMENT="statisticsCollection";
	private static final String GROUP_ELEMENT="statgroup";
	private static final String STATKEEPER_ELEMENT="stat";
	private static final String STATKEEPER_SUMMARY_ELEMENT="cumulative";
	private static final String STATKEEPER_INTERVAL_ELEMENT="interval";
	private static final String STATISTICS_XML_VERSION="1";

	private static final long PERIOD_ALLOWED_LENGTH_HOUR=1100*60*60; // 10% extra
	private static final long PERIOD_ALLOWED_LENGTH_DAY=PERIOD_ALLOWED_LENGTH_HOUR*24;
	private static final long PERIOD_ALLOWED_LENGTH_WEEK=PERIOD_ALLOWED_LENGTH_DAY*7;
	private static final long PERIOD_ALLOWED_LENGTH_MONTH=PERIOD_ALLOWED_LENGTH_DAY*31;
	private static final long PERIOD_ALLOWED_LENGTH_YEAR=PERIOD_ALLOWED_LENGTH_DAY*366;


	private static final String[] PERIOD_FORMAT_HOUR={"hour","HH"};
	private static final String[] PERIOD_FORMAT_DATEHOUR={"datehour","yyyy-MM-dd HH"};
	private static final String[] PERIOD_FORMAT_DAY={"day","dd"};
	private static final String[] PERIOD_FORMAT_DATE={"date","yyyy-MM-dd"};
	private static final String[] PERIOD_FORMAT_WEEKDAY={"weekday","E"};
	private static final String[] PERIOD_FORMAT_WEEK={"week","ww"};
	private static final String[] PERIOD_FORMAT_YEARWEEK={"yearweek","yyyy'W'ww"};
	private static final String[] PERIOD_FORMAT_MONTH={"month","MM"};
	private static final String[] PERIOD_FORMAT_YEARMONTH={"yearmonth","yyyy-MM"};
	private static final String[] PERIOD_FORMAT_YEAR={"year","yyyy"};

	private XmlBuilder root;
	private Date now;
	private Date mark;

	public StatisticsKeeperXmlBuilder(Date now, Date mark) {
		super();
		this.now=now;
		this.mark=mark;
	}

	public XmlBuilder getXml() {
		return root; 
	}

	public Object start() {
		log.debug("**********  start StatisticsKeeperXmlBuilder  **********");
		root = new XmlBuilder(ROOT_ELEMENT);
		root.addAttribute("version",STATISTICS_XML_VERSION);
		root.addAttribute("timestamp",DateUtils.format(now,DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("intervalStart",DateUtils.format(mark,DateUtils.FORMAT_GENERICDATETIME));
		root.addAttribute("host",Misc.getHostname());
		root.addAttribute("instance",AppConstants.getInstance().getProperty("instance.name"));
		addPeriodIndicator(root,new String[][]{PERIOD_FORMAT_HOUR,PERIOD_FORMAT_DATEHOUR},PERIOD_ALLOWED_LENGTH_HOUR);
		addPeriodIndicator(root,new String[][]{PERIOD_FORMAT_DAY,PERIOD_FORMAT_DATE,PERIOD_FORMAT_WEEKDAY},PERIOD_ALLOWED_LENGTH_DAY);
		addPeriodIndicator(root,new String[][]{PERIOD_FORMAT_WEEK,PERIOD_FORMAT_YEARWEEK},PERIOD_ALLOWED_LENGTH_WEEK);
		addPeriodIndicator(root,new String[][]{PERIOD_FORMAT_MONTH,PERIOD_FORMAT_YEARMONTH},PERIOD_ALLOWED_LENGTH_MONTH);
		addPeriodIndicator(root,new String[][]{PERIOD_FORMAT_YEAR},PERIOD_ALLOWED_LENGTH_YEAR);
		return root;
	}
	
	public void end(Object data) {
		log.debug("**********  end StatisticsKeeperXmlBuilder  **********");
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

	private void addPeriodIndicator(XmlBuilder xml, String[][] periods, long allowedLength) {
		long intervalStart=mark.getTime(); 
		long intervalEnd=now.getTime();
		if ((intervalEnd-intervalStart)<=allowedLength) {
			long midterm=(intervalEnd>>1)+(intervalStart>>1);
			for (int i=0; i<periods.length; i++) {
				String[] periodPair=periods[i];
				xml.addAttribute(periodPair[0],DateUtils.format(new Date(midterm),periodPair[1]));
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
