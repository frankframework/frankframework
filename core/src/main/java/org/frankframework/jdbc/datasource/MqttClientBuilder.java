package org.frankframework.jdbc.datasource;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MqttClientBuilder {
	private String name;
	private String url;
	private String username;
	private String password;
	private String clientId;
	private String persistenceDirectory;
	private boolean cleanSession;
	private boolean automaticReconnect;
	private int timeout;
	private int keepAliveInterval;
}
