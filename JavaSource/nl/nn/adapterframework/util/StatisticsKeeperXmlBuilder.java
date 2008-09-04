/*
 * $Log: StatisticsKeeperXmlBuilder.java,v $
 * Revision 1.3  2008-09-04 12:19:28  europe\L190409
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
