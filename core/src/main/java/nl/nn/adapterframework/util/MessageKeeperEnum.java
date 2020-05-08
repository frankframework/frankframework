package nl.nn.adapterframework.util;

public enum MessageKeeperEnum {

	
	INFO_LEVEL("INFO"),
	WARN_LEVEL("WARN"),
	ERROR_LEVEL("ERROR");
	
	private String level;
	
	MessageKeeperEnum(String level) {
		this.level = level;
	}
	
	public String getLevel() {
		return this.level;
	}
}
