/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.statistics.micrometer;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;

public class MetricsInitializer implements StatisticsKeeperIterationHandler<MetricsInitializer.NodeConfig> {

	
	private MeterRegistry registry;
	private NodeConfig root;

	protected class NodeConfig {
		String name;
		List<String> groupType;
		
		NodeConfig(String name, List<String> groupType) {
			this.name = name;
			this.groupType = groupType!=null ? groupType : new LinkedList<>();
		}
	}
	
	public MetricsInitializer(MeterRegistry registry, String name) {
		this.registry = registry;
		root = new NodeConfig(name, null);
	}
	
	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public NodeConfig start(Date now, Date mainMark, Date detailMark) throws SenderException {
		return root;
	}

	@Override
	public void end(NodeConfig data) throws SenderException {
	}

	@Override
	public void handleStatisticsKeeper(NodeConfig data, StatisticsKeeper sk) throws SenderException {
		sk.initMetrics(registry, data.name, data.groupType);
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, long value) throws SenderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, Date value) throws SenderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NodeConfig openGroup(NodeConfig parentData, String name, String type) throws SenderException {
		List<String> types = new LinkedList<>(parentData.groupType);
		types.add(type);
		return new NodeConfig(parentData.name+"."+name, types);
	}

	@Override
	public void closeGroup(NodeConfig data) throws SenderException {
	}

}
