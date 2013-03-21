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
/*
 * $Log: TestIfsaServiceExecute.java,v $
 * Revision 1.12  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.10  2009/12/31 10:06:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * SendJmsMessage/TestIfsaService/TestPipeLine: made zipfile-upload facility case-insensitive
 *
 * Revision 1.9  2009/09/03 08:47:27  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix: or upload or message
 *
 * Revision 1.8  2008/12/16 13:37:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * read messages in the right encoding
 *
 * Revision 1.7  2008/05/22 07:44:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.6  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.5  2005/06/28 09:03:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close in finally-clause
 *
 * Revision 1.4  2005/04/14 09:54:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * saved results
 *
 * Revision 1.3  2005/04/14 09:28:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * saved message protocols to form
 *
 * Revision 1.2  2005/04/14 09:12:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * exchanged message and result
 *
 * Revision 1.1  2005/04/14 08:07:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TestIfsaService-functionality
 *
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;


/**
 * Executes the sending of a test message to an IFSA Service.
 * 
 * @author  Gerrit van Brakel / Johan Verrips
 * @since   4.3
 * @version $Id$
 */
public final class TestIfsaServiceExecute extends ActionBase {
	public static final String version = "$RCSfile: TestIfsaServiceExecute.java,v $ $Revision: 1.12 $ $Date: 2011-11-30 13:51:45 $";
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	    
	
	    // Was this transaction cancelled?
	    // -------------------------------
	    if (isCancelled(request)) {
	        log.debug("testIfsaService was cancelled");
	        removeFormBean(mapping, request);
	        return (mapping.findForward("success"));
	    }
	
	    // Retrieve form content
	    // ---------------------
	    DynaActionForm sendIfsaMessageForm = (DynaActionForm) form;
	    String form_applicationId =(String) sendIfsaMessageForm.get("applicationId");
	    String form_serviceId = (String) sendIfsaMessageForm.get("serviceId");
	    String form_messageProtocol = (String) sendIfsaMessageForm.get("messageProtocol");
	    String form_message = (String) sendIfsaMessageForm.get("message");
	    FormFile form_file=(FormFile) sendIfsaMessageForm.get("file");

		String result="";

		// if upload is choosen, it prevails over the message
		if ((form_file != null) && (form_file.getFileSize() > 0)) {
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
			if (FileUtils.extensionEqualsIgnoreCase(form_file.getFileName(),"zip")) {
				ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(form_file.getFileData()));
				for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
					String name = entry.getName();
					int size = (int)entry.getSize();
					if (size>0) {
						byte[] b=new byte[size];
						int rb=0;
						int chunk=0;
						while (((int)size - rb) > 0) {
							chunk=archive.read(b,rb,(int)size - rb);
							if (chunk==-1) {
								break;
							}
							rb+=chunk;
						}
						String currentMessage = XmlUtils.readXml(b,0,rb,request.getCharacterEncoding(),false);
						result += name + ":";
						IfsaRequesterSender sender;
						try {
							// initiate MessageSender
							sender = new IfsaRequesterSender();
							try {
								sender.setName("testIfsaServiceAction");
								sender.setApplicationId(form_applicationId);
								sender.setServiceId(form_serviceId);
								sender.setMessageProtocol(form_messageProtocol);
			
								sender.configure();
								sender.open();
								sender.sendMessage(name+"_" + Misc.createSimpleUUID(), currentMessage);
								result += "success\n";
							} catch (Throwable t) {
								error("error occured sending message",t);
								result += "failure\n";
							} finally {
								sender.close();
							}
						} catch (Exception e) {
							error("error occured on creating object or closing connection",e);
						}
					}
					archive.closeEntry();
				}
				archive.close();
				form_message = null;
			} else {
				form_message = XmlUtils.readXml(form_file.getFileData(),request.getCharacterEncoding(),false);
			}
		} else {
			form_message=new String(form_message.getBytes(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		}
			
		if(form_message != null && form_message.length() > 0) {
			IfsaRequesterSender sender;
			result="";
			try {
				// initiate MessageSender
				sender = new IfsaRequesterSender();
				try {
					sender.setName("testIfsaServiceAction");
					sender.setApplicationId(form_applicationId);
					sender.setServiceId(form_serviceId);
					sender.setMessageProtocol(form_messageProtocol);
			
					sender.configure();
					sender.open();
					result = sender.sendMessage("testmsg_"+Misc.createUUID(),form_message);
				} catch (Throwable t) {
					error("error occured sending message",t);
				} finally {
					sender.close();
				}
			} catch (Exception e) {
				error("error occured on creating object or closing connection",e);
			}
		}
	
		StoreFormData(form_message, result, sendIfsaMessageForm);
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
		    saveErrors(request, errors);
	        return (new ActionForward(mapping.getInput()));
	    }
	
		//Successfull: store cookie
	          String cookieValue = "";
	          cookieValue += "applicationId=\"" + form_applicationId + "\"";
	          cookieValue += " "; //separator
	          cookieValue += "serviceId=\"" + form_serviceId + "\"";
	          cookieValue += " "; //separator          
	          cookieValue += "messageProtocol=\"" + form_messageProtocol + "\"";
	          Cookie sendIfsaCookie = new Cookie(AppConstants.getInstance().getProperty("WEB_IFSACOOKIE_NAME"), cookieValue);
	          sendIfsaCookie.setMaxAge(Integer.MAX_VALUE);
	          log.debug("Store cookie for " + request.getServletPath()+
	          " cookieName[" + AppConstants.getInstance().getProperty("WEB_IFSACOOKIE_NAME")+"] "+
	          " cookieValue[" + new StringTagger(cookieValue).toString()+"]");
	          try {
		          response.addCookie(sendIfsaCookie);
	          } catch (Throwable e) {
		          log.warn("unable to add cookie to request. cookie value ["+sendIfsaCookie.getValue()+"]");
	          }
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
	
	public void StoreFormData(String message, String result, DynaActionForm form){
		if (null!=message) form.set("message", message);
		if (null != result) {
			form.set("result", result);
		}
		List protocols=new ArrayList();
		protocols.add("RR");
		protocols.add("FF");
		form.set("messageProtocols", protocols);
	}
	
}
