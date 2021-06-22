package nl.nn.adapterframework.monitoring;

/*
 * See also the discussion above class Alarm. This class Trigger is introduced to allow
 * <Trigger alarm=${myVar}> in the Frank!Doc.
 */
public class Trigger extends TriggerBase {
	@Override
	public void setAlarm(boolean b) {
		super.setAlarm(b);
	}
}
