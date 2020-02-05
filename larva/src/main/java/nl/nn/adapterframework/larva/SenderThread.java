/*
   Copyright 2020 Integration Partners
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
package nl.nn.adapterframework.larva;

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

    public SenderThread(ISender sender, String request, boolean convertExceptionToMessage) {
		name = sender.getName();
		this.sender = sender;
        this.request = request;
        this.convertExceptionToMessage = convertExceptionToMessage;
		log.debug("Creating SenderThread for ISender '" + name + "'");
		log.debug("Request: " + request);
    }

	public SenderThread(ISenderWithParameters senderWithParameters, String request, ParameterResolutionContext parameterResolutionContext, boolean convertExceptionToMessage) {
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
