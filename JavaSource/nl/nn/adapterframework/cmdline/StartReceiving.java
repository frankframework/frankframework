package nl.nn.adapterframework.cmdline;

import nl.nn.adapterframework.receivers.JmsMessageReceiver;
import nl.nn.adapterframework.jms.JmsMessageListener;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;


/**
 *  
 * Configures a queue connection and receiver and starts listening to it.
 * @author     Johan Verrips
 * @created    14 februari 2003
 */
public class StartReceiving {
	public static final String version="$Id: StartReceiving.java,v 1.1 2004-02-04 08:36:13 a1909356#db2admin Exp $";
	

	static final String DFLT_ACKNOWLEDGEMODE = "client";
	static final boolean DFLT_PERSISTENT  = false;
	static final String DFLT_QCFN         = "QueueConnectionFactory";
	static final String DFLT_TCFN         = "TopicConnectionFactory";

    /**
	 *  The main program for the jms.JmsMessageReceiver class
	 *
	 * @param  args  The command line arguments. use -help to show options
	 */
	public static void main(String[] args) {

		CommandLine cmdline  = null;
		GnuParser gp         = new GnuParser();

		try {
			cmdline = gp.parse(makeOptions(), args);
		} catch (ParseException e) {
			HelpFormatter h  = new HelpFormatter();
			h.printHelp("this is help", makeOptions());
			System.out.println(e.getMessage());
			System.exit(1);
		}


		if (cmdline.hasOption("help")) {
			// the help option has been specified, print the usage
			// information
			usage();
		} else if (cmdline.hasOption("destination")) {
			
			// default values 
			String acknowledgeMode  = DFLT_ACKNOWLEDGEMODE;
			boolean persistent  = DFLT_PERSISTENT;
			String icf          = null;
			String providerURL  = null;
			

			// persistent
			int secs            = -1;
			// seconds to timeout

			// see if an ack mode has been specified. If it hasn't
			// then assume CLIENT_ACKNOWLEDGE mode.

			if (cmdline.hasOption("ackmode")) {
				acknowledgeMode=cmdline.getOptionValue("ackmode");
			}

			if (cmdline.hasOption("icf")) {
				icf = cmdline.getOptionValue("icf");
			}
			String destination_name   = cmdline.getOptionValue("destination");
			String destination_type   = cmdline.getOptionValue("desttype");
			
			if (destination_name != null) {

				if (cmdline.hasOption("url")) {
					providerURL = cmdline.getOptionValue("url");
				}

				if (cmdline.hasOption("persistent")) {
					persistent = true;
				}

				if (cmdline.hasOption("timeout")) {
					secs = Integer.parseInt(cmdline.getOptionValue("timeout"));
				}

				JmsMessageReceiver receiver   = new JmsMessageReceiver();
				JmsMessageListener listener   = (JmsMessageListener)(receiver.getListener());
				listener.setAcknowledgeMode(acknowledgeMode);
				listener.setDestinationName(destination_name);
				listener.setDestinationType(destination_type);
				if (cmdline.hasOption("qcfn")) 
					listener.setQueueConnectionFactoryName(cmdline.getOptionValue("qcfn"));
				else
					listener.setQueueConnectionFactoryName(DFLT_QCFN);
				if (cmdline.hasOption("tcfn")) 
					listener.setTopicConnectionFactoryName(cmdline.getOptionValue("tcfn"));
				else
					listener.setTopicConnectionFactoryName(DFLT_TCFN);
					
				listener.setDestinationName(destination_name);
				listener.setPersistent(persistent);
				//listener.setSecondsToTimeout(secs);
				if (null!=providerURL) listener.setProviderURL(providerURL);
				if (null!=icf) listener.setInitialContextFactoryName(icf)	;
					receiver.startRunning();
				

			} else {
				// anything else print the usage message
				usage();
			}
		}
	}
	/**
	 *  Function to return and define the options valid for the Command Line Interface (CLI) 
	 * @see org.apache.commons.cli.Options
	 * @return    Options object
	 */
	static Options makeOptions() {
		Options options  = new Options();
		options.addOption("destination", true, "destination name");
		options.addOption("desttype", true, "destination type, should be QUEUE or TOPIC");
		options.addOption("ackmode", true, "<auto | client | dups>, defaults to" +DFLT_ACKNOWLEDGEMODE);
		options.addOption("persistent", false, "specifies persistent delivery mode, default to "+DFLT_PERSISTENT);
		options.addOption("icf", true, "initial connection factory, you may also specify this by the -Djava.naming.factory.initial=<value> option ");
		options.addOption("url", true, "provider url, you may also specify this by the -Djava.naming.provider.url=value option)");		
		options.addOption("timeout", true, "seconds to wait before exiting");
		options.addOption("help", false, "display help");
		options.addOption("qcfn", true, "queue connection factory name, defaults to "+DFLT_QCFN);
		options.addOption("tcfn", true, "topic connection factory name, defaults to "+DFLT_TCFN);
		return options;
	}
	/**
	 * Print out information on running this sevice
	 */

	protected static void usage() {
		HelpFormatter h  = new HelpFormatter();
		h.printHelp("this is help", makeOptions());
	}
}
