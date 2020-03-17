package nl.nn.adapterframework.util;

public enum OracleKeyWords {

	NEXTVAL(".NEXTVAL"),
	CURRVAL(".CURRVAL"),
	FOR_UPDATE("FOR UPDATE"),
	EMPTY_CLOB("EMPTY_CLOB"),
	SYSDATE("SYSDATE"),
	SYSTIMESTAMP("SYSTIMESTAMP"),
	EMPTY_BLOB("EMPTY_BLOB");
	
	private final String key;
	
	OracleKeyWords(String key) {
		this.key = key;
	}
	
	public String key() {
		return this.key;
	}
	
}
