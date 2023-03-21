package com.metabase.app;

public interface SQLObjectTranspiler {
    default String generateSQL(Utils.SQLTranspilerType type) {
        switch (type) {
            case MY_SQL:
                return this.transpileToMySQL();
            case POSTGRESQL:
                return this.transpileToPostgreSQL();
            case SQL_SERVER:
                return this.transpileToServerSQL();
        }
        return null;
    }
    String transpileToPostgreSQL();
    String transpileToMySQL();
    String transpileToServerSQL();
}
