package nl.nn.adapterframework.http;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.ConnectionObserver;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class RestEtagMemcached implements IRestEtagCache {

	protected Logger log = LogUtil.getLogger(this);
	private MemcachedClient client = null;

	final ConnectionObserver obs = new ConnectionObserver() {
		public void connectionEstablished(SocketAddress sa, int reconnectCount) {
			String msg = "successfully established a memcache connection ["+sa+"]";
			if(reconnectCount > 1)
				msg += " reconnectCount ["+reconnectCount+"]";
			ConfigurationWarnings.getInstance().add(log, msg);
		}

		public void connectionLost(SocketAddress sa) {
			String msg = "lost memcached connection [" + sa + "] reconnecting...";
			ConfigurationWarnings.getInstance().add(log, msg);
		}
	};

	public RestEtagMemcached() {
		AppConstants ac = AppConstants.getInstance();
		String address = ac.getProperty("etag.cache.server", "localhost:11211");
		String username = ac.getProperty("etag.cache.username", "");
		String password = ac.getProperty("etag.cache.password", "");
		int timeout = ac.getInt("etag.cache.timeout", 10);

		List<InetSocketAddress> addresses = AddrUtil.getAddresses(address);

		ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder()
			.setProtocol(Protocol.BINARY)
			.setOpTimeout(timeout)
			.setInitialObservers(Collections.singleton(obs));

		if(addresses.size()  > 1)
			connectionFactoryBuilder.setFailureMode(FailureMode.Redistribute);
		else
			connectionFactoryBuilder.setFailureMode(FailureMode.Retry);

		if(!username.isEmpty())
			connectionFactoryBuilder.setAuthDescriptor(AuthDescriptor.typical(username, password));

		ConnectionFactory cf = connectionFactoryBuilder.build();

		try {
			client = new MemcachedClient(cf, addresses);
			//Fetching a none-existing key to test the connection
			Future<Object> future = client.asyncGet("test-connection");
			future.get(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			ConfigurationWarnings.getInstance().add(log, "Unable to connect to one or more memcached servers.", null, true);
		}
	}

	public Object get(String key) {
		try {
			return client.get(key);
		}
		catch(Exception e) {
			return null;
		}
	}

	public void put(String key, Object value) {
		client.set(key, 0, value);
	}

	public boolean remove(String key) {
		client.delete(key);
		return true;
	}

	public boolean containsKey(String key) {
		return (this.get(key) != null);
	}

	public void clear() {
		client.flush();
	}

	public void destroy() {
		client.shutdown();
	}
}
