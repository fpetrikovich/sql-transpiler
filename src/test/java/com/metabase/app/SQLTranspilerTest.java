package com.metabase.app;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;


@RunWith(value = Parameterized.class)
public class SQLTranspilerTest {

    private final String testName;
    private final Map<Integer, String> fieldMap;
    private final Map<String, Object> argsMap;
    private final Utils.SQLTranspilerType sqlType;
    private final String expected;

    // Inject via constructor
    public SQLTranspilerTest(String name, Utils.SQLTranspilerType sqlType, Map<Integer, String> fieldMap, Map<String, Object> argsMap , String expected) {
        this.testName = name;
        this.sqlType = sqlType;
        this.fieldMap = fieldMap;
        this.argsMap = argsMap;
        this.expected = expected;
    }

    @Parameters(name = "{index}: SQLTranspilerTest({0})")
    public static Collection<Object[]> data() {

        Map<Integer, String> fieldMap = new HashMap<Integer, String>() {{
            put(1, "id");
            put(2, "name");
            put(3, "date_joined");
            put(4, "age");
            put(5, "update-at");
        }};

        return Arrays.asList(new Object[][]{
                {
                        "Postgresql + where + null equality",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {"is-empty", new Object[]{"field", 3}});
                        }},
                        "SELECT * FROM data WHERE date_joined IS NULL;"
                },
                {
                        "Postgresql + where + number check >",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {">", new Object[]{"field", 4}, 35});
                        }},
                        "SELECT * FROM data WHERE age > 35;"
                },
                {
                        "Postgresql + where + nested and < =",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {Utils.AND_OP, new Object[] {"<", new Object[]{"field", 1}, 5}, new Object[] {"=", new Object[]{"field", 2}, "joe"}});
                        }},
                        "SELECT * FROM data WHERE id < 5 AND name = 'joe';"
                },
                {
                        "Postgresql + where + nested or != =",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {Utils.OR_OP, new Object[] {"!=", new Object[]{"field", 3}, "2015-11-01"}, new Object[] {"=", new Object[]{"field", 1}, 456}});
                        }},
                        "SELECT * FROM data WHERE date_joined <> '2015-11-01' OR id = 456;"
                },
                {
                        "Postgresql + where + nested and (!= or (> =))",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {Utils.AND_OP, new Object[] {"not-empty", new Object[]{"field", 3}}, new Object[] {Utils.OR_OP, new Object[] {">", new Object[]{"field", 4}, 25}, new Object[] {"=", new Object[]{"field", 2}, "Jerry"}}});
                        }},
                        "SELECT * FROM data WHERE date_joined IS NOT NULL AND (age > 25 OR name = 'Jerry');"
                },
                {
                        "Postgres + where + equality list",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {"=", new Object[]{"field", 4}, 25, 26, 27});
                        }},
                        "SELECT * FROM data WHERE age IN (25, 26, 27);"
                },
                {
                        "Postgres + where + equality 2 arguments",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {"=", new Object[]{"field", 2}, "cam"});
                        }},
                        "SELECT * FROM data WHERE name = 'cam';"
                },
                {
                        "MySQL + where + equality 2 arguments + limit",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[] {"=", new Object[]{"field", 2}, "cam"});
                            put(Utils.LIMIT_CLAUSE, 10);
                        }},
                        "SELECT * FROM data WHERE name = 'cam' LIMIT 10;"
                },
                {
                        "Postgresql + limit",
                        Utils.SQLTranspilerType.POSTGRESQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.LIMIT_CLAUSE, 10);
                        }},
                        "SELECT * FROM data LIMIT 10;"
                },
                {
                        "SQL Server + limit",
                        Utils.SQLTranspilerType.SQL_SERVER,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.LIMIT_CLAUSE, 20);
                        }},
                        "SELECT TOP 20 * FROM data;"
                },
                {
                        "SQL Server + optimize where clause",
                        Utils.SQLTranspilerType.SQL_SERVER,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{"is-empty", null});
                        }},
                        "SELECT * FROM data;"
                },
                {
                        "MySQL + where + simplify logical condition by removing OR",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.AND_OP, new Object[]{Utils.OR_OP, new Object[]{"is-empty", null}, new Object[]{"not-empty", new Object[]{"field", 3}}}, new Object[]{"=", new Object[]{"field", 2}, "cam"}});
                        }},
                        "SELECT * FROM data WHERE name = 'cam';"
                },
                {
                        "MySQL + where + simplify logical condition by removing AND and therefore OR",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.OR_OP, new Object[]{Utils.AND_OP, new Object[]{"is-empty", null}, new Object[]{"!=", 3, 4}}, new Object[]{"=", new Object[]{"field", 2}, "cam"}});
                        }},
                        "SELECT * FROM data;"
                },
                {
                        "MySQL + where + double negation optimization",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.OR_OP, new Object[]{"not", new Object[]{"not", new Object[]{"is-empty", new Object[]{"field", 3}}}}});
                        }},
                        "SELECT * FROM data WHERE date_joined IS NULL;"
                },
                {
                        "MySQL + where + negation",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.OR_OP, new Object[]{"not", new Object[]{"=", new Object[]{"field", 3}, "24-05-2022"}}});
                        }},
                        "SELECT * FROM data WHERE NOT date_joined = '24-05-2022';"
                },
                {
                        "MySQL + where + negation with is null optimization",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.OR_OP, new Object[]{"not", new Object[]{"is-empty", new Object[]{"field", 3}}}});
                        }},
                        "SELECT * FROM data WHERE date_joined IS NOT NULL;"
                },
                {
                        "MySQL + where + negation with is not null optimization",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{Utils.OR_OP, new Object[]{"not", new Object[]{"not-empty", new Object[]{"field", 3}}}});
                        }},
                        "SELECT * FROM data WHERE date_joined IS NULL;"
                },
                {
                        "MySQL + where + negation with always true statement",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{"not", new Object[]{"=", 3, 3}});
                        }},
                        "SELECT * FROM data WHERE NOT TRUE;"
                },
                {
                        "MySQL + where + escapes and quotes correctly",
                        Utils.SQLTranspilerType.MY_SQL,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{"not", new Object[]{"=", new Object[] {"field", 5}, "I've escaped!"}});
                        }},
                        "SELECT * FROM data WHERE NOT `update-at` = 'I\\\'ve escaped!';"
                },
                {
                        "SQL Server + where + escapes and quotes correctly",
                        Utils.SQLTranspilerType.SQL_SERVER,
                        fieldMap,
                        new HashMap<String, Object>() {{
                            put(Utils.WHERE_CLAUSE, new Object[]{"not", new Object[]{"=", new Object[] {"field", 5}, "I've escaped!"}});
                        }},
                        "SELECT * FROM data WHERE NOT \"update-at\" = 'I\\\'ve escaped!';"
                },
        });
    }

    @Test
    public void test_generateSQL() {
        SQLTranspiler transpiler = new SQLTranspiler(this.fieldMap, this.argsMap);

        String result = transpiler.generateSQL(this.sqlType);

        assertEquals(this.expected, result);
    }

}