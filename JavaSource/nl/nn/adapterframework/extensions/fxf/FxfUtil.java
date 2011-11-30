/*
 * $Log: FxfUtil.java,v $
 * Revision 1.5  2011-11-30 13:51:51  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/01/26 14:42:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new style ProcessUtil
 *
 * Revision 1.2  2009/03/04 15:56:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for FXF 2.0
 *
 * Revision 1.1  2008/09/04 12:05:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * test for version of fxf
 *
 */
package nl.nn.adapterframework.extensions.fxf;

import java.io.StringReader;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * FXF utility functions.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class FxfUtil {
	protected static Logger log = LogUtil.getLogger(FxfUtil.class);
	
	public static String getVersion(String script) {
		AppConstants ac = AppConstants.getInstance();
		
		String configured=ac.getResolvedProperty("fxf.version");
		String result="unknown";
		
		if ("auto".equalsIgnoreCase(configured)) {
			try {
				String command = script+" version";
				log.debug("checking FXF version by executing command ["+command+"]");
				String execResult=ProcessUtil.executeCommand(command,10);
				log.debug("output of command ["+execResult+"]");
				result=execResult;
			} catch (SenderException e) {
				log.debug("caught SenderException determining version of FXF: "+e.getMessage());
			} catch (Throwable t) {
				log.debug("caught ["+ClassUtils.nameOf(t)+"] determining version of FXF: "+t.getMessage());
			}
		} else {
			result=configured;
		}
		return result;
	}

	public static boolean isAtLeastVersion2(String script) {
		String version=getVersion(script);
		log.debug("FxF version ["+version+"]");
		if (StringUtils.isEmpty(version) || version.equals("unknown")) {
			return false;
		}
		int pointPos=version.indexOf('.');
		if (pointPos>0) {
			version=version.substring(0,pointPos);
		}
		int versionMajor=Integer.parseInt(version);
		return versionMajor>=2;
	}
	
	public static String makeProcessedPutFileMessage(String transportHandle ) {
		return makeMqMessage("Commit", transportHandle);
	}
	public static String makeProcessedGetFileMessage(String transportHandle ) {
		return makeMqMessage("Processed", transportHandle);
	}

	protected static String makeMqMessage(String action, String transportHandle ) {
		XmlBuilder mqMessage = new XmlBuilder("FXF");
		mqMessage.addAttribute("version","2.0");
		mqMessage.addAttribute("action",action);
		mqMessage.addAttribute("transporthandle",transportHandle);
		return mqMessage.toXML();
	}
	
	public static Trigger parseTrigger(String message) throws ListenerException {
		
		Trigger trigger = new Trigger();
		Digester digester = new Digester();
		digester.setUseContextClassLoader(true);

		digester.push(trigger);
 
		try {

			digester.addSetProperties("FXF");
			digester.addObjectCreate ("*/Transfer",Transfer.class);
			digester.addSetProperties("*/Transfer");
			digester.addSetNext      ("*/Transfer","registerTransfer");
				
			StringReader sr= new StringReader(message);
			InputSource is= new InputSource(sr);
				
			digester.parse(is);
			return trigger;

		} catch (Throwable t) {
			// wrap exception to be sure it gets rendered via the IbisException-renderer
			throw new ListenerException("error during parsing trigger message ["+message +"]", t);
		}
	}

}
