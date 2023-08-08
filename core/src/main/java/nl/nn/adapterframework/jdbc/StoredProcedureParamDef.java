package nl.nn.adapterframework.jdbc;

import java.sql.JDBCType;

import lombok.Getter;

class StoredProcedureParamDef {
	@Getter final int position;
	@Getter final JDBCType type;
	@Getter final String name;

	StoredProcedureParamDef(int position, JDBCType type) {
		this.position = position;
		this.type = type;
		this.name = String.valueOf(position);
	}

	StoredProcedureParamDef(int position, JDBCType type, String name) {
		this.position = position;
		this.type = type;
		this.name = name;
	}
}
