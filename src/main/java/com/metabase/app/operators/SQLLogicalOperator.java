package com.metabase.app.operators;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;

public final class SQLLogicalOperator implements SQLObjectTranspiler {

    private enum OperatorType {
        AND(Utils.AND_OP_UP), OR(Utils.OR_OP_UP);
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
    private final SQLObjectTranspiler[] operands;

    public SQLLogicalOperator(OperatorType operator, SQLObjectTranspiler[] operands) {
        this.operator = operator;
        this.operands = operands;
    }

    public OperatorType getOperator() {
        return this.operator;
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

    /**
     * Method will transcribe an operand of the logical statement, adding parenthesis if the
     * precedence of the child logical statement is less than that of the father operation.
     */
    private String transpileOperand(Utils.SQLTranspilerType type, SQLObjectTranspiler operand) {
        if (this.operator == OperatorType.AND
                && operand instanceof SQLLogicalOperator
                && ((SQLLogicalOperator) operand).getOperator() == OperatorType.OR
        ) {
            String operandQuery = operand.generateSQL(type);
            return !operandQuery.isEmpty()
                ? String.format("(%s)", operand.generateSQL(type))
                : "";
        }
        return operand.generateSQL(type);
    }

    private String transpileEquivalentQuery(Utils.SQLTranspilerType type) {
        StringBuilder query = new StringBuilder("");
        String operandQuery;

        // To know whether we have to remove from the end if the query is empty
        boolean containsOperator = false;

        for (int i = 0; i < this.operands.length; i++) {
            operandQuery = this.transpileOperand(type, this.operands[i]);

            // Empty means the query was optimized and is always true
            // => OR will always be true, AND will simply ignore
              if (operandQuery.isEmpty() && this.operator == OperatorType.OR) {
                return "";
            }

            // When it is not the last operand and the query is not empty, add it with the operator
            if (i < this.operands.length - 1 && !operandQuery.isEmpty()) {
                query.append(operandQuery).append(" ").append(this.operator).append(" ");
                containsOperator = true;
            }
            // If we are on the last operand, handle it separately
            else if (i == this.operands.length - 1) {

                // Remove last operator if query is empty
                if (operandQuery.isEmpty() && containsOperator) {
                    query.setLength(query.length() - this.operator.toString().length() - 2);
                }
                // Add the statement without a new operator if query is not empty
                else {
                    query.append(operandQuery);
                }
            }
        }

        return query.toString();
    }

    public static SQLLogicalOperator.OperatorType StringToSQLLogicalOperatorType(String op) {
        switch (op) {
            case Utils.AND_OP:
            case Utils.AND_OP_UP:
                return OperatorType.AND;
            case Utils.OR_OP:
            case Utils.OR_OP_UP:
                return OperatorType.OR;
            default:
                throw new IllegalArgumentException("Unknown logical operand " + op);
        }
    }
}
