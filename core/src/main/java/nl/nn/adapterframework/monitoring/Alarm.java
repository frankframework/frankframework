package nl.nn.adapterframework.monitoring;

/*
 * This derived class of TriggerBase is no different from TriggerBase itself. It is introduced
 * purely on behalf of the Frank!Doc. If Monitor.registerAlarm would take a TriggerBase
 * instead of an Alarm, then the Frank!Element TriggerBase would appear with multiple
 * element roles. That would cause a name clash. This name clash would also disappear
 * when an interface ITriggerBase would be introduced, but that would cause a name clash
 * between ITrigger and TriggerBase. The latter name clash would produce a generic element
 * option Alarm with a default className pointing to Java class TriggerBase. That generic
 * element option would have a anyAttribute, so the attributes would no longer
 * be checked.
 * 
 */
public class Alarm extends TriggerBase {
	public Alarm() {
		setAlarm(true);
	}
}
