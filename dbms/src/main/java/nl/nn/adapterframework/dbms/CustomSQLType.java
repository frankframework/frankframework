package nl.nn.adapterframework.dbms;

import java.sql.SQLType;

class CustomSQLType implements SQLType {

	private final String vendor;
	private final int jdbcTypeNr;

	public CustomSQLType(String vendor, int jdbcTypeNr) {
		this.vendor = vendor;
		this.jdbcTypeNr = jdbcTypeNr;
	}

	@Override
	public String getName() {
		return "CURSOR";
	}

	@Override
	public String getVendor() {
		return vendor;
	}

	@Override
	public Integer getVendorTypeNumber() {
		return jdbcTypeNr;
	}
}
