package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsMessageSender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;

/**
 * Send a message with a JMS Queue. Command line interface.
 * @author Johan Verrips
 */
public class StartSender {
	public static final String version="$Id: StartSender.java,v 1.1 2004-02-04 08:36:13 a1909356#db2admin Exp $";
	
	
    static final String DFLT_DESTINATIONNAME 	= "queue1";
    static final String DFLT_DESTINATIONTYPE 	= "QUEUE";
    static final String DFLT_QCFN		= "QueueConnectionFactory";
    static final String DFLT_TCFN		= "TopicConnectionFactory";
    static final String DFLT_ACKNOWLEDGEMODE="client";
    static final int DFLT_REPEAT        = 1; //send only once

public static String fileToString(String fName) throws IOException {
    String content = "";
    BufferedReader brin = null;
    try {
        brin = new BufferedReader(new FileReader(fName));
        String nextLine;
        while (null != (nextLine = brin.readLine()))
            content += nextLine + "\n";
        brin.close();
    } catch (IOException e) {
        try {
            if (null != brin) {
                brin.close();
            }
        } catch (IOException ex) {
	        System.out.println("IOException for file ["+fName+"]: "+ex.getMessage()+
		         "\ncaught while closing, after other IOException: "+e.getMessage());
        }
        throw e;
    }
    return content;
}
    public static void main(String[] args) {

   		
		String message="";
	    String destinationName = DFLT_DESTINATIONNAME;
	    String acknowledgeMode=DFLT_ACKNOWLEDGEMODE;
	    String correlationId = null;
        int repeat=DFLT_REPEAT;
   	    String icf = null;
	    String providerURL = null;



        CommandLine cl = null;
        GnuParser gp = new GnuParser();

        try {
            cl = gp.parse(makeOptions(), args);

        } catch (ParseException e) {
            HelpFormatter h = new HelpFormatter();

            h.printHelp("this is help", makeOptions());
            System.out.println(e.getMessage());
            System.exit(1);
        }
        if ((cl.hasOption("message")) && (cl.hasOption("file"))){
            System.out.println("message and file cannot be both specified!");
            HelpFormatter h = new HelpFormatter();
            h.printHelp("this is help", makeOptions());
            return;
        }
			if (cl.hasOption("icf")) {
				icf = cl.getOptionValue("icf");
			}
			if (cl.hasOption("url")) {
				providerURL = cl.getOptionValue("url");
			}


        if (cl.hasOption("destination")) destinationName = cl.getOptionValue("destination");
        if (cl.hasOption("message")) message = cl.getOptionValue("message");
        if (cl.hasOption("cid")) correlationId = cl.getOptionValue("cid");
        if (cl.hasOption("repeat")) repeat = Integer.parseInt(cl.getOptionValue("repeat"));

        if (cl.hasOption("file"))
        {   String filename = cl.getOptionValue("file");
            try{
                message=fileToString(filename);
            } catch (IOException e){
                System.out.println(e.getMessage());
                return;
            }

        }

        if (cl.hasOption("acknowledgeMode"))
        {   acknowledgeMode = cl.getOptionValue("acknowledgeMode");
        }


		// fire messageSender and exit
        JmsMessageSender ms = new JmsMessageSender();
        ms.setDestinationName(destinationName);
        ms.setPersistent(cl.hasOption("persistent"));
	    ms.setAcknowledgeMode(acknowledgeMode);
	    if (cl.hasOption("desttype"))
		    ms.setDestinationType(cl.getOptionValue("desttype"));
	    if (cl.hasOption("tcfn"))
		    ms.setTopicConnectionFactoryName(cl.getOptionValue("tcfn"));
		else
			ms.setTopicConnectionFactoryName(DFLT_TCFN);
        if (cl.hasOption("replyto")) ms.setReplyToName(cl.getOptionValue("replyto"));

		if (cl.hasOption("qcfn"))
		    ms.setQueueConnectionFactoryName(cl.getOptionValue("qcfn"));
		else
			ms.setQueueConnectionFactoryName(DFLT_QCFN);
			    
        if (null!=providerURL) ms.setProviderURL(providerURL);
		if (null!=icf) ms.setInitialContextFactoryName(icf)	;

		try {
        for (int t=0; t<repeat; t++){
	        
            	ms.sendMessage(correlationId, message);
	        
        }
	    ms.close();
	} catch (SenderException se) {
			se.printStackTrace();
	}
}
    public static Options makeOptions() {
        Options options = new Options();

        options.addOption("destination", true, "destination name, defaults to " + DFLT_DESTINATIONNAME);
        options.addOption("desttype", true, "destination type, either QUEUE or TOPIC " + DFLT_DESTINATIONTYPE);
        options.addOption("message", true, "message to send");
        options.addOption("persistent", false, "should persistent sending be used");
        options.addOption("qcfn", true, "queue connection factory name, defaults to \"" + DFLT_QCFN + "\"");
        options.addOption("tcfn", true, "topic connection factory name, defaults to \"" + DFLT_TCFN + "\"");
        options.addOption("cid", true, "Correlation ID");
        options.addOption("replyto", true, "give a reply to name in the message");
        options.addOption("ackmode", true, "<auto | client | dups> defaults to "+DFLT_ACKNOWLEDGEMODE);
		options.addOption("icf", true, "initial connection factory, you may also specify this by the -Djava.naming.factory.initial=<value> option ");
		options.addOption("url", true, "provider url, you may also specify this by the -Djava.naming.provider.url=value option)");		
        
        options.addOption("help", false, "display this help");
        options.addOption("file", true, "file to send");
        options.addOption("repeat", true, "number of times to send the message or file");

        return options;
    }
}
