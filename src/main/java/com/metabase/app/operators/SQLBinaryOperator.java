package com.metabase.app.operators;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;

public final class SQLBinaryOperator implements SQLObjectTranspiler {
    private final String operator;
    private final SQLObjectTranspiler operand1;
    private final SQLObjectTranspiler operand2;

    public SQLBinaryOperator(String operator, SQLObjectTranspiler operand1, SQLObjectTranspiler operand2) {
        this.operator = operator;
        this.operand1 = operand1;
        this.operand2 = operand2;
    }

    @Override
    public String transpileToPostgreSQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.POSTGRESQL);
    }

    @Override
    public String transpileToMySQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.POSTGRESQL);
    }

    @Override
    public String transpileToServerSQL() {
        return this.transpileEquivalentQuery(Utils.SQLTranspilerType.SQL_SERVER);
    }

    private String transpileEquivalentQuery(Utils.SQLTranspilerType type) {
        return String.format("%s %s %s",
                this.operand1.generateSQL(type),
                this.operator,
                this.operand2.generateSQL(type)
        );
    }
}
