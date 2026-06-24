/*
  Copyright 2026 WeAreFrank!

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
package org.frankframework.messaging.amqp;

import java.util.Map;

import org.apache.commons.lang3.Strings;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RabbitMqHelper {
	private RabbitMqHelper() {
		// don't construct util class
	}

	/**
	 * Returns {@code true} if the server represented by the given connection properties is RabbitMQ 4 or higher.
	 * RabbitMQ 4+ requires AMQP 1.0 v2 address format and does not support the legacy v1 address format by default.
	 */
	static boolean isRabbitMq4(Map<?, ?> connectionProperties) {
		if (!isRabbitMq(connectionProperties)) {
			return false;
		}

		Object version = connectionProperties.get("version");

		if (version instanceof String versionStr) {
			try {
				int majorVersion = Integer.parseInt(versionStr.split("\\.")[0]);
				return majorVersion >= 4;
			} catch (NumberFormatException e) {
				log.warn("Could not parse RabbitMQ version '{}', assuming v2 address format is not required", versionStr);
			}
		}
		return false;
	}

	static boolean isRabbitMq(Map<?, ?> connectionProperties) {
		if (connectionProperties == null) {
			return false;
		}

		return "RabbitMQ".equals(connectionProperties.get("product"));
	}

	static boolean isReplyUsingV1Format(String replyAddress) {
		return replyAddress != null && Strings.CS.contains(replyAddress, "/") && !replyAddress.startsWith("/queues/") && !replyAddress.startsWith("/exchanges/");
	}

	static boolean isAddressUsingV1Format(String address) {
		return Strings.CS.contains(address, "/") && !address.startsWith("/queues/") && !address.startsWith("/exchanges/");
	}
}
