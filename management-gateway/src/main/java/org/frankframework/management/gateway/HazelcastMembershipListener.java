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
package org.frankframework.management.gateway;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.gateway.events.ClusterMemberEvent;

public class HazelcastMembershipListener implements MembershipListener {
	private final ApplicationContext applicationContext;

	HazelcastMembershipListener(ApplicationContext applicationContext) {
		this.applicationContext	= applicationContext;
	}

	@Override
	public void memberAdded(MembershipEvent e) {
		applicationContext.publishEvent(new ClusterMemberEvent(applicationContext, ClusterMemberEvent.EventType.ADD_MEMBER, mapMember(e.getMember())));
	}

	@Override
	public void memberRemoved(MembershipEvent e) {
		applicationContext.publishEvent(new ClusterMemberEvent(applicationContext, ClusterMemberEvent.EventType.REMOVE_MEMBER, mapMember(e.getMember())));
	}

	static OutboundGateway.ClusterMember mapMember(Member member) {
		OutboundGateway.ClusterMember cm = new OutboundGateway.ClusterMember();
		cm.setAddress(member.getSocketAddress().getHostName() + ":" + member.getSocketAddress().getPort());
		cm.setId(member.getUuid());
		Map<String, String> attrs = new HashMap<>(member.getAttributes());
		String type = attrs.remove(HazelcastConfig.ATTRIBUTE_TYPE_KEY);
		if (StringUtils.isNotBlank(type)) {
			if (HazelcastConfig.InstanceType.WORKER.name().equals(type)) {
				cm.setType("worker");
			} else {
				cm.setType("console");
			}
		}
		cm.setAttributes(attrs);
		cm.setLocalMember(member.localMember());
		cm.setName(attrs.containsKey(HazelcastConfig.ATTRIBUTE_APPLICATION_KEY) ? attrs.get(HazelcastConfig.ATTRIBUTE_APPLICATION_KEY) : member.getUuid()
				.toString());
		return cm;
	}
}
