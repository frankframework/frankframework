/*
 * $Log: StatisticsKeeperLogger.java,v $
 * Revision 1.4  2008-05-14 09:30:43  europe\L190409
 * simplified methodnames of StatisticsKeeperIterationHandler
 *
 * Revision 1.3  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2006/02/09 08:02:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * iterate over string scalars too
 *
 * Revision 1.1  2005/12/28 08:31:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced StatisticsKeeper-iteration
 *
 */
package nl.nn.adapterframework.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Logs statistics-keeper contents to log
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 * @version Id
 */
public class StatisticsKeeperLogger implements StatisticsKeeperIterationHandler {
	public static final String version="$RCSfile: StatisticsKeeperLogger.java,v $ $Revision: 1.4 $ $Date: 2008-05-14 09:30:43 $";
	protected Logger log = LogUtil.getLogger("nl.nn.adapterframework.statistics");

	public Object start() {
		log.info("********** start statistics dump **********");
		return null;
	}
	public void end(Object data) {
		log.info("**********  end statistics dump  **********");
	}

	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) {
		String msg=sk.getName();
		for (int i=0;i<sk.getItemCount();i++) {
			msg+=" "+sk.getItemName(i)+"="+sk.getItemValue(i)+";";
		}
		log.info((String)data+" "+msg);
	}

	public void handleScalar(Object data, String scalarName, long value){
		handleScalar(data,scalarName,""+value);
	}

	public void handleScalar(Object data, String scalarName, String value){
		log.info((String)data+" "+scalarName+"="+value+";");
	}

	public Object openGroup(Object parentData, String name, String type) {
		String result="";
		if (StringUtils.isNotEmpty((String)parentData)) {
			result+=(String)parentData+"/";
		}
		result+=type+"("+name+")";
		log.debug(result+":");
		return result;
	}

	public void closeGroup(Object data) {
		log.debug((String)data+".");
	}

}
