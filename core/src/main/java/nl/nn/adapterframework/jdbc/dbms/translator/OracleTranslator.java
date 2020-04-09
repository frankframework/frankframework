package nl.nn.adapterframework.jdbc.dbms.translator;

public class OracleTranslator extends ITranslator {

	public OracleTranslator(ITranslator target) {
		super(target);
	}

	public OracleTranslator() {
		super();
	}

	@Override
	protected void populateMaps() {
		regexes.put("NEXTVAL", toPattern("(\\w+)\\.NEXTVAL"));
		replacements.put("NEXTVAL", "$1.NEXTVAL");

		regexes.put("CURRVAL", toPattern("(\\w+)\\.CURRVAL"));
		replacements.put("CURRVAL", "$1.CURRVAL");

		regexes.put("EMPTY_BLOB", toPattern("EMPTY_BLOB\\(\\)"));
		replacements.put("EMPTY_BLOB", "EMPTY_BLOB()");

		regexes.put("EMPTY_CLOB", toPattern("EMPTY_CLOB\\(\\)"));
		replacements.put("EMPTY_CLOB", "EMPTY_CLOB()");

		regexes.put("SYSDATE", toPattern("SYSDATE"));
		replacements.put("SYSDATE", "SYSDATE");

		regexes.put("SYSTIMESTAMP", toPattern("SYSTIMESTAMP"));
		replacements.put("SYSTIMESTAMP", "SYSTIMESTAMP");

		regexes.put("DUAL", toPattern("FROM DUAL"));

		regexes.put("FOR UPDATE", toPattern("FOR UPDATE"));
		replacements.put("FOR UPDATE", "FOR UPDATE");

		regexes.put("LIMIT", toPattern("FETCH FIRST [0-9]+ ROWS ONLY"));
		replacements.put("LIMIT", "FETCH FIRST $1 ROWS ONLY");
	}
}
