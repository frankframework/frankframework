package nl.nn.adapterframework.jdbc.dbms.translator;

public class MysqlTranslator extends ITranslator {
	public MysqlTranslator(ITranslator target) {
		super(target);
	}

	public MysqlTranslator() {
		super();
	}

	@Override
	protected void populateMaps() {
		replacements.put("NEXTVAL", "NULL");
		replacements.put("EMPTY_BLOB", "NULL");
		replacements.put("EMPTY_CLOB", "NULL");

		regexes.put("SYSDATE", toPattern("SYSDATE()"));
		replacements.put("SYSDATE", "SYSDATE()");

		regexes.put("SYSTIMESTAMP", toPattern("CURRENT_TIMESTAMP()"));
		replacements.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP()");

		regexes.put("FOR UPDATE", toPattern("FOR UPDATE"));
		replacements.put("FOR UPDATE", "FOR UPDATE");

		regexes.put("LIMIT", toPattern("LIMIT ([0-9]+)"));
		replacements.put("LIMIT", "LIMIT $1");

		regexes.put("LIMIT", toPattern("LIMIT ([0-9]+)(,[0-9]+)?"));
		replacements.put("LIMIT", "LIMIT $1,$2");
	}
}
