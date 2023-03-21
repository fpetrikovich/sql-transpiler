package com.metabase.app.variables;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;

public class SQLVariable implements SQLObjectTranspiler {
    private final Object variable;
    private final boolean isStrLiteral;

    public SQLVariable(Object variable, boolean isStrLiteral) {
        this.variable = variable;
        this.isStrLiteral = isStrLiteral;
    }

    public Object getVariable() {
        return this.variable;
    }

    public boolean isFieldName() {
        return this.variable instanceof String && !isStrLiteral;
    }

    @Override
    public String transpileToPostgreSQL() {
        if (this.isFieldName() && this.variable.toString().matches(".*[ -.+~|].*")) {
            return String.format("\"%s\"", this.variable);
        }
        return this.transpileEquivalentQuery();
    }

    @Override
    public String transpileToMySQL() {
        if (this.isFieldName() && this.variable.toString().matches(".*[ -.+~|].*")) {
            return String.format("`%s`", this.variable);
        }
        return this.transpileEquivalentQuery();
    }

    @Override
    public String transpileToServerSQL() {
        if (this.isFieldName() && this.variable.toString().matches(".*[ -.+~|].*")) {
            return String.format("\"%s\"", this.variable);
        }
        return this.transpileEquivalentQuery();
    }

    private String transpileEquivalentQuery() {
        if (this.variable == null) {
            return "NULL";
        } else if (this.variable instanceof String && isStrLiteral) {
            String s = (String) this.variable;
            s = s.replaceAll("'", "\\\\'");
            s = s.replaceAll("\"", "\\\\\"");
            return String.format("'%s'", s);
        } else {
            return this.variable.toString();
        }
    }
}
