package com.metabase.app;

import com.metabase.app.clauses.SQLLimitClause;
import com.metabase.app.clauses.SQLWhereClause;
import com.metabase.app.operators.SQLBinaryOperator;
import com.metabase.app.operators.SQLEqualityOperator;
import com.metabase.app.operators.SQLLogicalOperator;
import com.metabase.app.operators.SQLUnaryOperator;
import com.metabase.app.variables.SQLVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public final class SQLTranspiler implements SQLObjectTranspiler
{
    private final Map<Integer, String> fieldMap;
    private final Map<String, Object> argsMap;
    private final Map<String, SQLObjectTranspiler> sqlClausesMap = new HashMap<>();


    public SQLTranspiler(Map<Integer, String> fieldMap, Map<String, Object> argsMap) {
        this.fieldMap = fieldMap;
        this.argsMap = argsMap;

        this.createSQLClausesMap();
    }


    @Override
    public String transpileToPostgreSQL() {
        StringBuilder query = new StringBuilder("SELECT * FROM data");
        if (this.sqlClausesMap.containsKey(Utils.WHERE_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.WHERE_CLAUSE).generateSQL(Utils.SQLTranspilerType.POSTGRESQL));
        }
        if (this.sqlClausesMap.containsKey(Utils.LIMIT_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.LIMIT_CLAUSE).generateSQL(Utils.SQLTranspilerType.POSTGRESQL));
        }
        query.append(";");
        return query.toString();
    }

    @Override
    public String transpileToMySQL() {
        StringBuilder query = new StringBuilder("SELECT * FROM data");
        if (this.sqlClausesMap.containsKey(Utils.WHERE_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.WHERE_CLAUSE).generateSQL(Utils.SQLTranspilerType.MY_SQL));
        }
        if (this.sqlClausesMap.containsKey(Utils.LIMIT_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.LIMIT_CLAUSE).generateSQL(Utils.SQLTranspilerType.MY_SQL));
        }
        query.append(";");
        return query.toString();
    }

    @Override
    public String transpileToServerSQL() {
        StringBuilder query = new StringBuilder("SELECT");
        if (this.sqlClausesMap.containsKey(Utils.LIMIT_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.LIMIT_CLAUSE).generateSQL(Utils.SQLTranspilerType.SQL_SERVER));
        }
        query.append(" ").append("* FROM data");
        if (this.sqlClausesMap.containsKey(Utils.WHERE_CLAUSE)) {
            query.append(this.sqlClausesMap.get(Utils.WHERE_CLAUSE).generateSQL(Utils.SQLTranspilerType.SQL_SERVER));
        }
        query.append(";");
        return query.toString();
    }

    private void createSQLClausesMap() {
        this.argsMap.forEach((k, v) -> {
            if (k.equals(Utils.WHERE_CLAUSE)) {
                sqlClausesMap.put(k, new SQLWhereClause(this.convertToSQLObject(v)));
            }
            else if (k.equals(Utils.LIMIT_CLAUSE)) {
                sqlClausesMap.put(k, new SQLLimitClause(this.convertToSQLObject(v)));
            }
        });
    }

    private SQLObjectTranspiler convertToSQLObject(Object obj) {

        if (obj instanceof Object[] && ((Object[]) obj).length > 0 && ((Object[]) obj)[0] instanceof String) {
            Object[] argsList = ((Object[]) obj);
            String op = (String) argsList[0];
            switch (op) {
                // Field case
                case "field":
                    if (argsList[1] instanceof Integer) {
                        return new SQLVariable(this.fieldMap.get((Integer) argsList[1]), false);
                    }
                    break;
                // Unary operators
                case "is-empty":
                case "not-empty":
                case "not":
                    // check 1 other args
                    if (argsList.length != 2) {
                        throw new IllegalArgumentException("Unary operators can't have more than one operand.");
                    }
                    return new SQLUnaryOperator(
                            SQLUnaryOperator.StringToOperatorType(op),
                            this.convertToSQLObject(argsList[1])
                    );
                // Logical operators
                case Utils.AND_OP:
                case Utils.OR_OP:
                    if (argsList.length < 2) {
                        throw new IllegalArgumentException("Logical operators must have at least 1 operand.");
                    }
                    SQLObjectTranspiler[] logicalOperands = new SQLObjectTranspiler[argsList.length-1];

                    for (int i = 1; i < argsList.length; i++) {
                        logicalOperands[i-1] = this.convertToSQLObject(argsList[i]);
                    }
                    return new SQLLogicalOperator(
                          SQLLogicalOperator.StringToSQLLogicalOperatorType(op),
                          logicalOperands
                    );
                case "<":
                case ">":
                    // check 2 other args
                    if (argsList.length != 3) {
                        throw new IllegalArgumentException("Binary operators must have two operand.");
                    }
                    return new SQLBinaryOperator(
                            op,
                            this.convertToSQLObject(argsList[1]),
                            this.convertToSQLObject(argsList[2])
                    );
                case "=":
                case "!=":
                    // check >2 other args
                    if (argsList.length < 3) {
                        throw new IllegalArgumentException("Equality operators must have at least 2 operands.");
                    }
                    SQLObjectTranspiler[] equalityOperands = new SQLObjectTranspiler[argsList.length-2];
                    SQLObjectTranspiler mainOperand = this.convertToSQLObject(argsList[1]);

                    for (int i = 2; i < argsList.length; i++) {
                        equalityOperands[i-2] = this.convertToSQLObject(argsList[i]);
                    }
                    return new SQLEqualityOperator(
                            SQLEqualityOperator.StringToSQLEqualityOperatorType(op),
                            mainOperand,
                            equalityOperands
                    );
                default:
                    throw new IllegalArgumentException("Invalid SQL query!");
            }
        } else {
            return new SQLVariable(obj, obj instanceof String);
        }

        return null;
    }
}
