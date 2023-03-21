package com.metabase.app.clauses;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;

public final class SQLLimitClause implements SQLObjectTranspiler {
    private final SQLObjectTranspiler clause;

    public SQLLimitClause(SQLObjectTranspiler clause) {
        this.clause = clause;
    }

    @Override
    public String transpileToPostgreSQL() {
        return String.format(" LIMIT %s", this.clause.generateSQL(Utils.SQLTranspilerType.POSTGRESQL));
    }

    @Override
    public String transpileToMySQL() {
        return String.format(" LIMIT %s", this.clause.generateSQL(Utils.SQLTranspilerType.MY_SQL));
    }

    @Override
    public String transpileToServerSQL() {
        return String.format(" TOP %s", this.clause.generateSQL(Utils.SQLTranspilerType.MY_SQL));
    }
}
