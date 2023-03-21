package com.metabase.app.operators;

import com.metabase.app.SQLObjectTranspiler;
import com.metabase.app.Utils;
import com.metabase.app.variables.SQLVariable;

import java.util.Objects;
import java.util.stream.Stream;

public final class SQLEqualityOperator implements SQLObjectTranspiler {
    private enum OperatorType {
        EQUALS("="), NOT_EQUALS("<>");
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
    private final SQLObjectTranspiler mainOperand;
    private final SQLObjectTranspiler[] equalityOperands;

    public SQLEqualityOperator(OperatorType op, SQLObjectTranspiler mainOperand, SQLObjectTranspiler[] operands) {
        this.operator = op;
        this.mainOperand = mainOperand;
        this.equalityOperands = operands;
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

    private SQLObjectTranspiler optimizeEqualityQuery() {
        if (this.mainOperand instanceof SQLVariable && !((SQLVariable) this.mainOperand).isFieldName()) {
            Stream<Object> operandStream = Stream.of(this.equalityOperands).map(o -> ((SQLVariable) o).getVariable());

            // Will always be true if none match the main operand
            if (this.operator == OperatorType.NOT_EQUALS && operandStream.noneMatch(operand -> Objects.equals(((SQLVariable) this.mainOperand).getVariable(), operand))) {
                return null;
            }
            // Will always be true if one element is equal to the main operand
            if (this.operator == OperatorType.EQUALS && operandStream.anyMatch(operand -> Objects.equals(((SQLVariable) this.mainOperand).getVariable(), operand))) {
                return null;
            }
        }
        return this;
    }
    private String transpileEquivalentQuery(Utils.SQLTranspilerType type) {
        // Optimize by
        if (this.optimizeEqualityQuery() != null) {
            // Acts as a binary operator if there is only 1 comparison
            if (this.equalityOperands.length == 1) {
                return String.format("%s %s %s",
                        this.mainOperand.generateSQL(type),
                        this.operator,
                        this.equalityOperands[0].generateSQL(type)
                );
            }
            // Case where there is a list of elements to compare it with
            if (this.operator == OperatorType.EQUALS) {
                return String.format("%s IN (%s)", this.mainOperand.generateSQL(type), this.buildArgumentList(type));
            } else {
                return String.format("%s NOT IN (%s)", this.mainOperand.generateSQL(type), this.buildArgumentList(type));
            }
        } else {
            return "";
        }
    }

    private String buildArgumentList(Utils.SQLTranspilerType type) {
        StringBuilder strList = new StringBuilder("");
        strList.append(this.equalityOperands[0].generateSQL(type));

        for (int i = 1; i < this.equalityOperands.length; i++) {
            strList.append(", ").append(this.equalityOperands[i].generateSQL(type));
        }

        return strList.toString();
    }

    public static SQLEqualityOperator.OperatorType StringToSQLEqualityOperatorType(String op) {
        switch (op) {
            case "=":
                return OperatorType.EQUALS;
            case "!=":
                return OperatorType.NOT_EQUALS;
            default:
                throw new IllegalArgumentException("Unknown equality operand " + op);
        }
    }
}
