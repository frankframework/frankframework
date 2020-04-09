package nl.nn.adapterframework.jdbc.dbms.translator;

public class PostgresqlTranslator extends ITranslator {
	public PostgresqlTranslator(ITranslator target) {
		super(target);
	}

	public PostgresqlTranslator() {
		super();
	}

	@Override
	protected void populateMaps() {
		regexes.put("NEXTVAL", toPattern("NEXTVAL\\\\('(.*)'\\\\)"));
		replacements.put("NEXTVAL", "NEXTVAL('$1')");

		regexes.put("CURRVAL", toPattern("CURRVAL\\\\('(.*)'\\\\)"));
		replacements.put("CURRVAL", "CURRVAL('$1')");

		replacements.put("EMPTY_BLOB", "''");
		replacements.put("EMPTY_CLOB", "''");

		regexes.put("SYSDATE", toPattern("CURRENT_DATE"));
		replacements.put("SYSDATE", "CURRENT_DATE");

		regexes.put("SYSTIMESTAMP", toPattern("CURRENT_TIMESTAMP"));
		replacements.put("SYSTIMESTAMP", "CURRENT_TIMESTAMP");

		regexes.put("FOR UPDATE", toPattern("FOR UPDATE"));
		replacements.put("FOR UPDATE", "FOR UPDATE");

		regexes.put("LIMIT", toPattern("LIMIT ([0-9]+)"));
		replacements.put("LIMIT", "LIMIT $1");
	}
}
