package com.metabase.app;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        System.out.println("\n[RUNNING SQL TRANSPILER]");

        Map<Integer, String> fieldMap = new HashMap<Integer, String>() {{
            put(1, "id");
            put(2, "name");
            put(3, "date_joined");
            put(4, "age");
        }};
        
        SQLTranspiler transpiler = new SQLTranspiler(fieldMap, new HashMap<String, Object>() {{
            put(Utils.WHERE_CLAUSE, new Object[] {"=", new Object[]{"field", 2}, "cam"});
            put(Utils.LIMIT_CLAUSE, 10);
        }});

        System.out.println("\nGenerating SQL query for:");
        System.out.println("(generate-sql :sql-server fields {:where [:= [:field 2] \"cam\"], :limit 10})");
        System.out.println(transpiler.generateSQL(Utils.SQLTranspilerType.SQL_SERVER));

        transpiler = new SQLTranspiler(fieldMap, new HashMap<String, Object>() {{
            put(Utils.WHERE_CLAUSE, new Object[] {"=", new Object[]{"field", 4}, 25, 26, 27});
            put(Utils.LIMIT_CLAUSE, 10);
        }});

        System.out.println("\nGenerating SQL query for:");
        System.out.println("(generate-sql :mysql fields {:where [:= [:field 4] 25 26 27], :limit 10})");
        System.out.println(transpiler.generateSQL(Utils.SQLTranspilerType.MY_SQL));

        transpiler = new SQLTranspiler(fieldMap, new HashMap<String, Object>() {{
            put(Utils.WHERE_CLAUSE, new Object[]{Utils.AND_OP, new Object[]{Utils.OR_OP, new Object[]{"is-empty", null}, new Object[]{"not-empty", new Object[]{"field", 3}}}, new Object[]{"=", new Object[]{"field", 2}, "cam"}});
        }});

        System.out.println("\nGenerating SQL query for:");
        System.out.println("(generate-sql :mysql fields {:where [:and [:or [:is-empty null] [:not-empty [\"field\", 3]] [:= [:field 2] \"cam\"]})");
        System.out.println(transpiler.generateSQL(Utils.SQLTranspilerType.MY_SQL));

        System.out.println("\nCheck out the SQLTranspilerTest file for multiple SQL generation examples.");
    }
}
