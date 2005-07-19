/*
 * $Log: ShowConfigurationStatus.java,v $
 * Revision 1.5  2005-07-19 12:09:21  europe\L190409
 * added indication of inProcessStorage and errorStorage
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

/**
 * Prepare the main screen of the IbisConsole.
 * 
 * @version Id
 * @author  Johan Verrips
 */

public final class ShowConfigurationStatus extends ActionBase {
	public static final String version = "$RCSfile: ShowConfigurationStatus.java,v $ $Revision: 1.5 $ $Date: 2005-07-19 12:09:21 $";

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


        // Initialize action
        initAction(request);

        if (null==config) {
          return (mapping.findForward("noconfig"));
        }

        // retrieve adapters
        Iterator registeredAdapters=config.getRegisteredAdapterNames();
        


        XmlBuilder adapters=new XmlBuilder("registeredAdapters");
        while (registeredAdapters.hasNext()){

          String adapterName=(String)registeredAdapters.next();
          Adapter adapter= (Adapter) config.getRegisteredAdapter(adapterName);
          XmlBuilder adapterXML=new XmlBuilder("adapter");

          RunStateEnum adapterRunState = adapter.getRunState();
          
          adapterXML.addAttribute("name",adapter.getName());
          adapterXML.addAttribute("started", ""+(adapterRunState.equals(RunStateEnum.STARTED)));
          adapterXML.addAttribute("state", adapterRunState.toString());
          adapterXML.addAttribute("configured", ""+adapter.configurationSucceeded());
          adapterXML.addAttribute("upSince", adapter.getStatsUpSince());
          adapterXML.addAttribute("lastMessageDate", adapter.getLastMessageDate());

          Iterator recIt=adapter.getReceiverIterator();
          if (recIt.hasNext()){
	          XmlBuilder receiversXML=new XmlBuilder("receivers");
	          while (recIt.hasNext()){
		          IReceiver receiver=(IReceiver) recIt.next();
		          XmlBuilder receiverXML=new XmlBuilder("receiver");
		          receiversXML.addSubElement(receiverXML);

		          RunStateEnum receiverRunState = receiver.getRunState();
		           
		          receiverXML.addAttribute("isStarted", ""+(receiverRunState.equals(RunStateEnum.STARTED)));
		          receiverXML.addAttribute("state", receiverRunState.toString());
		          receiverXML.addAttribute("name",receiver.getName());
			      receiverXML.addAttribute("class", receiver.getClass().toString());
				  receiverXML.addAttribute("messagesReceived", ""+receiver.getMessagesReceived());
				  if (receiver instanceof ReceiverBase ) {
					ReceiverBase rb = (ReceiverBase) receiver;
					receiverXML.addAttribute("hasInprocessStorage", ""+(rb.getInProcessStorage()!=null));
					receiverXML.addAttribute("hasErrorStorage", ""+(rb.getErrorStorage()!=null));
				  }

				  if (receiver instanceof HasSender) {
					  ISender sender = ((HasSender) receiver).getSender();
			          if (sender != null) 
				          	receiverXML.addAttribute("senderName", sender.getName());
		          }
	          }
	         adapterXML.addSubElement(receiversXML); 
          }
	          

          adapterXML.addAttribute("messagesProcessed", ""+adapter.getNumOfMessagesProcessed());
          adapterXML.addAttribute("messagesInError", ""+adapter.getNumOfMessagesInError());

		
  		  // retrieve messages from adapters        
          XmlBuilder adapterMessages=new XmlBuilder("adapterMessages");
          for (int t=0; t<adapter.getMessageKeeper().size(); t++){
 	          XmlBuilder adapterMessage=new XmlBuilder("adapterMessage");
              adapterMessage.setCdataValue(adapter.getMessageKeeper().getMessage(t).getMessageText());
              adapterMessage.addAttribute("date", DateUtils.format(adapter.getMessageKeeper().getMessage(t).getMessageDate(), DateUtils.FORMAT_GENERICDATETIME));
			  adapterMessages.addSubElement(adapterMessage);
	          
          }
          adapterXML.addSubElement(adapterMessages);


          adapters.addSubElement(adapterXML);

        }
	    request.setAttribute("adapters", adapters.toXML());



        // Forward control to the specified success URI
        log.debug("forward to success");
        return (mapping.findForward("success"));

    }
}
