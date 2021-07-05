package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.doc.FrankDocGroup;

/*
 * This interface has been introduced on behalf of the Frank!Doc. This
 * interface acts as the argument of config child setters where the
 * Frank!Framework needs a TriggerBase. By using this interface
 * instead of TriggerBase as the argument type, we can have a singleconfig child
 * setter for Alarm, Clearing and Trigger. Without this interface, three
 * different config child setters would be needed because the algorithm
 * would produce singleton element types for the three mentioned classes.
 */
@FrankDocGroup(name = "Monitoring", order = 80)
public interface ITrigger {
	void registerAdapterFilter(AdapterFilter af);
	void setMonitor(Monitor monitor);
}
