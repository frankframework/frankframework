package nl.nn.adapterframework.statistics;import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

public abstract class MetricBase<M extends Meter> {

	protected M meter;

	public abstract void initMetrics(MeterRegistry registry, Iterable<Tag> tags, String name);

}
