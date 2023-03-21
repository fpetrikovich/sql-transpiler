package com.metabase.app;

public class Utils {
    public static enum SQLTranspilerType {
        MY_SQL,
        POSTGRESQL,
        SQL_SERVER
    }

    public final static String WHERE_CLAUSE = "where";
    public final static String LIMIT_CLAUSE = "limit";
    public final static String AND_OP = "and";
    public final static String OR_OP = "or";
    public final static String AND_OP_UP = "AND";
    public final static String OR_OP_UP = "OR";
}
