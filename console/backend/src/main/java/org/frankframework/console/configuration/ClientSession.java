/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.configuration;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import lombok.Getter;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;

@Component
@SessionScope
public class ClientSession implements InitializingBean {

	@Autowired
	@Qualifier("outboundGateway")
	private OutboundGateway outboundGateway;

	/**
	 * Get target or `NULL` when no target has been specified or `afterPropertiesSet` has not been called yet.
	 */
	@Nullable
	private @Getter UUID memberTarget;

	public void setMemberTarget(UUID id) {
		this.memberTarget = id;
	}

	public void setMemberTarget(String id) {
		setMemberTarget(UUID.fromString(id));
	}

	// When a new session is created, assign a default target
	@Override
	public void afterPropertiesSet() throws Exception {
		List<ClusterMember> members = outboundGateway.getMembers();
		members.stream().filter(m -> "worker".equals(m.getType())).findFirst().ifPresent(m -> {
			m.setSelectedMember(true);
			setMemberTarget(m.getId());
		});
	}
}
