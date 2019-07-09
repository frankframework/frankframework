package nl.nn.adapterframework.senders;

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.filesystem.IFileSystemListener;

public interface IExchangeMailListener extends IFileSystemListener<Item> {

	public void setMailAddress(String mailaddress);
	public void setUserName(String username);
	public void setPassword(String password);
	public void setUrl(String baseurl);
	public void setBaseFolder(String folder);
	public void setFilter(String filter);
}
