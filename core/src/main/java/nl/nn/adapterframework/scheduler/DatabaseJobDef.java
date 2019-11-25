package nl.nn.adapterframework.scheduler;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.senders.IbisLocalSender;

public class DatabaseJobDef extends JobDef {

	private String message = "";

	public void configure() throws ConfigurationException {
		try {
			setFunction(JobDefFunctions.SEND_MESSAGE.getName());
		} catch (ConfigurationException e) {}

		super.configure(null);
	}

	@Override
	protected void executeSendMessageJob(IbisManager ibisManager) {
		try {
			// send job
			IbisLocalSender localSender = new IbisLocalSender();
			localSender.setJavaListener(getReceiverName());
			localSender.setIsolated(false);
			localSender.setName("AdapterJob");
			if (getInterval() == 0) {
				localSender.setDependencyTimeOut(-1);
			}
			if (StringUtils.isNotEmpty(getAdapterName())) {
				IAdapter iAdapter = ibisManager.getRegisteredAdapter(getAdapterName());
				Configuration configuration = iAdapter.getConfiguration();
				localSender.setConfiguration(configuration);
			}
			localSender.configure();
			localSender.open();
			try {
				localSender.sendMessage(null, message);
			}
			finally {
				localSender.close();
			}
		}
		catch(Exception e) {
			getMessageKeeper().add("error while sending message (as part of scheduled job execution)", e);
		}
	}

	public void setMessage(String message) {
		this.message = message;
	}
	public String getMessage() {
		return message;
	}
}
