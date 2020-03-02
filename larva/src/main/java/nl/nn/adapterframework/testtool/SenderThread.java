package nl.nn.adapterframework.testtool;

import java.io.IOException;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @author Jaco de Groot
 */
public class SenderThread extends Thread {
    private static Logger log = LogUtil.getLogger(SenderThread.class);
    
    private String name;
    private ISender sender;
	private ISenderWithParameters senderWithParameters;
	private IPipeLineSession session;
    private String request;
    private String response;
    private SenderException senderException;
    private IOException ioException;
    private TimeOutException timeOutException;
    private boolean convertExceptionToMessage = false;

    SenderThread(ISender sender, String request, boolean convertExceptionToMessage) {
		name = sender.getName();
		this.sender = sender;
        this.request = request;
        this.convertExceptionToMessage = convertExceptionToMessage;
		log.debug("Creating SenderThread for ISender '" + name + "'");
		log.debug("Request: " + request);
    }

	SenderThread(ISenderWithParameters senderWithParameters, String request, IPipeLineSession session, boolean convertExceptionToMessage) {
		name = senderWithParameters.getName();
		this.senderWithParameters = senderWithParameters;
		this.request = request;
		this.session = session;
		this.convertExceptionToMessage = convertExceptionToMessage;
		log.debug("Creating SenderThread for ISenderWithParameters '" + name + "'");
		log.debug("Request: " + request);
	}

    public void run() {
        try {
        	if (senderWithParameters == null) {
				response = sender.sendMessage(new Message(request), session).asString();
        	} else {
				response = senderWithParameters.sendMessage(new Message(request),  session).asString();
        	}
        } catch(SenderException e) {
        	if (convertExceptionToMessage) {
        		response = Util.throwableToXml(e);
        	} else {
				log.error("SenderException for ISender '" + name + "'", e);
				senderException = e;
        	}
        } catch(IOException e) {
			if (convertExceptionToMessage) {
				response = Util.throwableToXml(e);
			} else {
				log.error("IOException for ISender '" + name + "'", e);
				ioException = e;
			}
        } catch(TimeOutException e) {
			if (convertExceptionToMessage) {
				response = Util.throwableToXml(e);
			} else {
				log.error("timeOutException for ISender '" + name + "'", e);
				timeOutException = e;
			}
        }
    }

    public String getResponse() {
    	log.debug("Getting response for Sender: " + name);
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return response;
    }

    public SenderException getSenderException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return senderException;
    }

    public IOException getIOException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return ioException;
    }

    public TimeOutException getTimeOutException() {
        while (this.isAlive()) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
            }
        }
        return timeOutException;
    }

}
