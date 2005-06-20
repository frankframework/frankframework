/*
 * $Log: ForEachChildElementPipe.java,v $
 * Revision 1.1  2005-06-20 09:08:40  europe\L190409
 * introduction of ForEachChildElementPipe
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message to a Sender for each child element of the input XML.
 *
 * For configuration options, see {@link MessageSendingPipe}.
 * 
 * @author Gerrit van Brakel
 * @since 4.3
 * 
 * $Id: ForEachChildElementPipe.java,v 1.1 2005-06-20 09:08:40 europe\L190409 Exp $
 */
public class ForEachChildElementPipe extends MessageSendingPipe {
	public static final String version="$RCSfile: ForEachChildElementPipe.java,v $ $Revision: 1.1 $ $Date: 2005-06-20 09:08:40 $";

	private boolean elementsOnly=true;

	private TransformerPool tp;

	public void configure() throws ConfigurationException {
		super.configure();
		try {
			tp=new TransformerPool(XmlUtils.IDENTITY_TRANSFORM);
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(e);
		}
	}

	public void start() throws PipeStartException  {
		try {
			tp.open();
		} catch (Exception e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	public void stop()   {
		tp.close();
		super.stop();
	}
	

	protected String sendMessage(Object input, PipeLineSession session, String correlationID, ISender sender, HashMap threadContext) throws SenderException, TimeOutException {
		// sendResult has a messageID for async senders, the result for sync senders
		String result=null;
		ISenderWithParameters psender=null;
		if (sender instanceof ISenderWithParameters && getParameterList()!=null) {
			psender = (ISenderWithParameters) sender;
		}		
		try {
			int count=0;
			Element fullMessage = XmlUtils.buildElement((String)input);
			Node node=fullMessage.getFirstChild();
			while (node!=null) { 
				if (elementsOnly) {
					while (node!=null && !(node instanceof Element)) { 
						node=node.getNextSibling();
					}
				}
				if (node!=null) {
					DOMSource src = new DOMSource(node);
					String item=tp.transform(src,null);
					if (log.isDebugEnabled()) {
						//log.debug(getLogPrefix(session)+"set current item to ["+item+"]");
						log.debug(getLogPrefix(session)+"sending item no ["+(++count)+"] element name ["+node.getNodeName()+"]");
					} 
					if (psender!=null) {
						//result = psender.sendMessage(correlationID, item, new ParameterResolutionContext(src, session));
						//TODO find out why ParameterResolutionContext cannot be constructed using dom-source
						result = psender.sendMessage(correlationID, item, new ParameterResolutionContext(item, session));
					} else {
						result = sender.sendMessage(correlationID, item);
					}
					node=node.getNextSibling();
				}
			}
			if (!sender.isSynchronous()) {
				return count+"";
			}
			return result;
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix(session)+"cannot parse input",e);
		} catch (TransformerException e) {
			throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
		}
	}


	public void setSender(ISender sender) {
		super.setSender(sender);
	}

}
