package com.metabase.app.clauses;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;

public final class SQLWhereClause implements SQLObjectTranspiler {
    private final SQLObjectTranspiler clause;

    public SQLWhereClause(SQLObjectTranspiler clause) {
        this.clause = clause;
    }

    @Override
    public String transpileToPostgreSQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.POSTGRESQL);
    }

    @Override
    public String transpileToMySQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.MY_SQL);
    }

    @Override
    public String transpileToServerSQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.SQL_SERVER);
    }

    private String transpileEquivalentQuery(Utils.SQLTranspilerType type) {
        String whereQuery = this.clause.generateSQL(type);
        return !whereQuery.isEmpty()
            ? String.format(" WHERE %s", whereQuery)
            : "";
    }
}
