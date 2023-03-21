package com.metabase.app.operators;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;
import com.metabase.app.variables.SQLVariable;

public final class SQLUnaryOperator implements SQLObjectTranspiler {
    private enum OperatorType {
        NOT("NOT"), IS_NULL("IS NULL"), IS_NOT_NULL("IS NOT NULL");
        private final String SQLString;
        private OperatorType(String SQLString) {
            this.SQLString = SQLString;
        }

        @Override
        public String toString(){
            return SQLString;
        }
    }
    private final OperatorType operator;
    private final SQLObjectTranspiler operand;

    public SQLUnaryOperator(OperatorType op, SQLObjectTranspiler operand) {
        this.operator = op;
        this.operand = operand;
    }

    public OperatorType getOperator() {
        return this.operator;
    }

    public SQLObjectTranspiler getOperand() {
        return this.operand;
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
        SQLObjectTranspiler optimizedQuery = this.optimizeUnaryQuery();

        if (optimizedQuery == null) return "";
        else if (optimizedQuery.equals(this)) {
            String operandQuery = this.operand.generateSQL(type);
            switch (this.operator) {
                case NOT:
                    return String.format("%s %s", OperatorType.NOT, operandQuery.isEmpty() ? "TRUE" : operandQuery);
                case IS_NOT_NULL:
                case IS_NULL:
                    return String.format("%s %s", this.operand.generateSQL(type), this.operator);
                default:
                    throw new IllegalStateException("Unknown unary operator did not throw exception in string to enum translation.");
            }
        } else {
            return optimizedQuery.generateSQL(type);
        }
    }

    private SQLObjectTranspiler optimizeUnaryQuery() {
        if (this.operator == OperatorType.NOT && this.operand instanceof SQLUnaryOperator) {
            OperatorType childOperator = ((SQLUnaryOperator) this.operand).getOperator();
            SQLObjectTranspiler childOperand = ((SQLUnaryOperator) this.operand).getOperand();

            switch (childOperator) {
                case NOT:
                    return childOperand;
                case IS_NULL:
                    return new SQLUnaryOperator(OperatorType.IS_NOT_NULL, childOperand);
                case IS_NOT_NULL:
                    return new SQLUnaryOperator(OperatorType.IS_NULL, childOperand);
            }
        }
        else if (this.operator == OperatorType.IS_NULL && this.operand instanceof SQLVariable && !((SQLVariable) this.operand).isFieldName() && ((SQLVariable) this.operand).getVariable() == null) {
            return null;
        }
        else if (this.operator == OperatorType.IS_NOT_NULL && this.operand instanceof SQLVariable && !((SQLVariable) this.operand).isFieldName() && ((SQLVariable) this.operand).getVariable() != null) {
            return null;
        }
        return this;
    }

    public static OperatorType StringToOperatorType(String op) {
        switch (op) {
            case "not":
                return OperatorType.NOT;
            case "is-empty":
                return OperatorType.IS_NULL;
            case "not-empty":
                return OperatorType.IS_NOT_NULL;
            default:
                throw new IllegalArgumentException("Unknown unary operand " + op);
        }
    }
}
