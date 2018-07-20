package nl.nn.ibistesttool.transform;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.testtool.transform.MessageTransformer;

import org.apache.commons.lang.StringUtils;

/**
 * Hide the same data as is hidden in the Ibis logfiles based on the
 * log.hideRegex property in log4j4ibis.properties.
 * 
 * @author Jaco de Groot
 */
public class HideRegexMessageTransformer implements MessageTransformer {
	String hideRegex;

	HideRegexMessageTransformer() {
		hideRegex = LogUtil.getLog4jHideRegex();
	}

	public String transform(String message) {
		if (message != null) {
			if (StringUtils.isNotEmpty(hideRegex)) {
				message = Misc.hideAll(message, hideRegex);
			}

			String threadHideRegex = LogUtil.getThreadHideRegex();
			if (StringUtils.isNotEmpty(threadHideRegex)) {
				message = Misc.hideAll(message, threadHideRegex);
			}
		}
		return message;
	}

	public String getHideRegex() {
		return hideRegex;
	}

	public void setHideRegex(String string) {
		hideRegex = string;
	}

}
