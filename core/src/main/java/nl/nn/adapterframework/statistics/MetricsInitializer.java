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
package nl.nn.adapterframework.statistics;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.EhCache2Metrics;
import net.sf.ehcache.Ehcache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;

public class MetricsInitializer implements StatisticsKeeperIterationHandler<MetricsInitializer.NodeConfig> {


	private MeterRegistry registry;
	private NodeConfig root;

	protected class NodeConfig {
		public String name;
		public List<Tag> tags;
		public int groupLevel;

		NodeConfig(String name, List<Tag> tags, int groupLevel) {
			this.name = name;
			this.tags = tags!=null ? tags : new LinkedList<>();
			this.groupLevel = groupLevel;
		}
	}

	public MetricsInitializer(MeterRegistry registry) {
		this.registry = registry;
		root = new NodeConfig("frank", null,0);
	}

	@Override
	public void configure() throws ConfigurationException {
		//not used
	}

	@Override
	public NodeConfig start(Date now, Date mainMark, Date detailMark) throws SenderException {
		return root;
	}

	@Override
	public void end(NodeConfig data) throws SenderException {
		//not used
	}

	@Override
	public void handleStatisticsKeeper(NodeConfig data, StatisticsKeeper sk) throws SenderException {
		if (sk==null) {
			System.out.println ("---> StatisticsKeeper is null");
			return;
		}
		if (data==null) {
			System.out.println ("---> data is null, sk="+sk.getName());
			sk.initMetrics(registry, sk.getName(), null);
			return;
		}
		sk.initMetrics(registry, data.name, data.tags);
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, ScalarMetricBase<?> meter) throws SenderException {
		meter.initMetrics(registry, data.name, data.tags, scalarName);
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, long value) throws SenderException {
		//not used
	}

	@Override
	public void handleScalar(NodeConfig data, String scalarName, Date value) throws SenderException {
		//not used
	}

	@Override
	public NodeConfig openGroup(NodeConfig parentData, String dimensionName, String type) throws SenderException {
		String nodeName = parentData.name;
		List<Tag> tags = new LinkedList<>(parentData.tags);
		int groupLevel = parentData.groupLevel;
		if (StringUtils.isNotEmpty(dimensionName)) {
			tags.add(Tag.of(type, dimensionName));
		} else {
			nodeName=nodeName+"."+type;
		}
		return new NodeConfig(nodeName, tags, groupLevel);
	}

	@Override
	public void closeGroup(NodeConfig data) throws SenderException {
		//not used
	}

	public void configureCache(Ehcache cache) {
		new EhCache2Metrics(cache, root.tags).bindTo(registry);
	}

}
