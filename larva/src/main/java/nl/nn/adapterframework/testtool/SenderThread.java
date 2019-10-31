package nl.nn.adapterframework.testtool;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 */
public class SenderThread extends Thread {
    private static Logger log = LogUtil.getLogger(SenderThread.class);
    
    private String name;
    private ISender sender;
	private ISenderWithParameters senderWithParameters;
	private ParameterResolutionContext parameterResolutionContext;
    private String request;
    private String response;
    private SenderException senderException;
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

	SenderThread(ISenderWithParameters senderWithParameters, String request, ParameterResolutionContext parameterResolutionContext, boolean convertExceptionToMessage) {
		name = senderWithParameters.getName();
		this.senderWithParameters = senderWithParameters;
		this.parameterResolutionContext = parameterResolutionContext;
		this.request = request;
		this.convertExceptionToMessage = convertExceptionToMessage;
		log.debug("Creating SenderThread for ISenderWithParameters '" + name + "'");
		log.debug("Request: " + request);
	}

    public void run() {
        try {
        	if (senderWithParameters == null) {
				response = sender.sendMessage(TestTool.TESTTOOL_CORRELATIONID, request);
        	} else {
				response = senderWithParameters.sendMessage(TestTool.TESTTOOL_CORRELATIONID, request, parameterResolutionContext);
        	}
        } catch(SenderException e) {
        	if (convertExceptionToMessage) {
        		response = Util.throwableToXml(e);
        	} else {
				log.error("SenderException for ISender '" + name + "'", e);
				senderException = e;
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
