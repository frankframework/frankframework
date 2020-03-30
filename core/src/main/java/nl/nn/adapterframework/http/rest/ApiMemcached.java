/*
Copyright 2017 Integration Partners B.V.

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
package nl.nn.adapterframework.http.rest;

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

public class ApiMemcached implements IApiCache {

	protected Logger log = LogUtil.getLogger(this);
	private MemcachedClient client = null;

	final ConnectionObserver obs = new ConnectionObserver() {
		@Override
		public void connectionEstablished(SocketAddress sa, int reconnectCount) {
			String msg = "successfully established a memcache connection to ["+sa+"]";
			if(reconnectCount > 1)
				msg += " after ["+reconnectCount+"] retries";
			log.info(msg);
		}

		@Override
		public void connectionLost(SocketAddress sa) {
			String msg = "lost memcached connection [" + sa + "] reconnecting...";
			log.error(msg);
		}
	};

	public ApiMemcached() {
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
			ConfigurationWarnings.add(log, "Unable to connect to one or more memcached servers.");
		}
	}

	@Override
	public Object get(String key) {
		try {
			return client.get(key);
		}
		catch(Exception e) {
			return null;
		}
	}

	@Override
	public void put(String key, Object value) {
		client.set(key, 0, value);
	}

	@Override
	public void put(String key, Object value, int ttl) {
		client.set(key, ttl, value);
	}

	@Override
	public boolean remove(String key) {
		client.delete(key);
		return true;
	}

	@Override
	public boolean containsKey(String key) {
		return (this.get(key) != null);
	}

	@Override
	public void clear() {
		client.flush();
	}

	@Override
	public void destroy() {
		client.shutdown();
	}
}
