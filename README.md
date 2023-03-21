# Metabase SQL Transpiler
### The problem

Your challenge is to write & test a basic transpiler which outputs a SQL string given a structured expression. **You
should make it possible for others to extend your code and add support for new filter clause types, new literal types,
or new dialects without having to modify the original code.**

You should provide a working and tested function called `generate-sql` with the following signature:

```clj
(generate-sql dialect fields query)
```

- `dialect` is the keyword name of the SQL dialect to generate SQL for. For the purposes of this exercise, it will be
  either `:sqlserver`, `:postgres`, or `:mysql`.
- `fields` is a map of integer IDs to a Field name. e.g.
  ```clj
  {1 "name", 2 "location}
  ```
- `query` is a map containing information about the limits and filters to include in the query you generate.

### Query DSL

For the purposes of this exercise you will be taking the `query` map and generating a SQL query like

```sql
SELECT * FROM data;
```

and adding `WHERE` and
[`LIMIT`](https://www.postgresql.org/docs/11/queries-limit.html)/[`TOP`](https://docs.microsoft.com/en-us/sql/t-sql/queries/top-transact-sql?view=sql-server-2017)
clauses as appropriate. For example, you might generate one of the following queries:

```sql
SELECT * FROM data WHERE name = 'cam';          -- Postgres/MySQL w/ WHERE
SELECT * FROM data WHERE name = 'cam' LIMIT 10; -- MySQL w/ WHERE & LIMIT
SELECT * FROM data LIMIT 20;                    -- Postgres w/ LIMIT
SELECT TOP 20 * FROM data;                      -- SQL Server w/ LIMIT
```

##### `query`

`query` is a map with the following schema. `:limit` and `:where` are both optional keys.

```clj
{:limit <unsigned-int>
 :where <where-clause>}
```

##### `where` clause

`:where`, when present, is a vector defining what should go in the `WHERE` clause of the SQL you generate. It has the
form:

```clj
[<operator> <args>+]

operator ::= :and | :or | :not | :< | :> | := | :!= | :is-empty | :not-empty
```

##### Operators

###### `:and` and `:or`

`:and` and `:or` are used purely as conjunctions and should support 1 or more arguments representing subclauses that
contain other operators. These clauses can also be nested with other compound clauses.

```clj
[:and <where-clause> <where-clause>]                      ; logical conjunction e.g. SQL `AND` operator
[:or <where-clause> <where-clause>]                       ; logical disjunction (SQL `OR`)
[:and [:or <where-clause> <where-clause>] <where-clause>] ; nested compound clauses

;; this is considered legal -- treat it as just `<where-clause>`
[:and <where-clause>]
```

###### `:not`

`:not` is similar to `:and` or `:or` but for obvious reasons always wraps a single clause.

```clj
[:not <where-clause>]
```

There are different ways to negate `WHERE` clauses in SQL, so how you do it is up to you.

###### `:<` and `:<`

`:<` and `:<` both take exactly two args. Args are either references to Fields, discussed more below, or number
literals.

```clj
;; arg ::= <field> | <number>
[:< <arg> <arg] ; x < y
```

###### `:=` and `:!=`

`:=` and `:!=` operate almost the same way, but the value is not necessarily a number, and they can accept more than 2
args. In the 2-arg form, they work the same way as `:<` and `:>`. With more that 2 args, they correspond to SQL `IN` and
`NOT IN` operators, respectively.

```clj
;; arg ::= <field> | <number> | <string> | nil
[:= <x> <y>]      ; x = y
[:!= <x> <y>]     ; x <> y
[:= <x> <y> <z>]  ; x IN (y, z)
[:!= <x> <y> <z>] ; x NOT IN (y, z)
```

Note that values might be `nil`, and all the implications that has.

###### `:is-empty` and `:not-empty`

`:is-empty` and `:not-empty` take a single argument and translate into the appropriate natural SQL syntax for `IS NULL`
and `IS NOT NULL`. The argument can be anything accepted by `:=` or `:!=`.

```clj
;; arg ::= <field> | <number> | <string> | nil
[:is-empty <arg>] ; field IS NULL
```

##### `field` clause

Fields always have the form

```clj
[:field <unsigned-int>]
```

The integer in question corresponds to one of the keys in the `:fields` map; you should replace it with the appropriate
identifier in the generated SQL.

### Query Optimization

Optimize out comparisons that are always logically true, for example

```clj
(generate-sql :sqlserver {} {:where [:is-empty nil], :limit 10})
;; [optimize out WHERE NULL IS NULL]
;; -> "SELECT TOP 10 * FROM data;"
```

and

```clj
[:not [:not x]]` ; -> x
[:not [:is-empty x]] -> [:not-empty x]
[:not [:not-empty x]] -> [:is-empty x]
```

Optimize out the OR queries that have 1 or more always logically true comparison

```
(generate-sql :sqlserver {} {:where [:or [:is-empty nil] [not-empty [:field 3]], :limit 10})
;; [optimize out WHERE NULL IS NULL OR date_joined IS NOT NULL]
;; -> "SELECT TOP 10 * FROM data;"
```

#### Examples

```clj
(generate-sql :postgres {1 :id, 2 :name} {:where [:= [:field 2] "cam"]})
;; -> "SELECT * FROM data WHERE name = 'cam';"

(generate-sql :mysql {1 :id, 2 :name} {:where [:= [:field 2] "cam"], :limit 10})
;; -> "SELECT * FROM data WHERE name = 'cam' LIMIT 10;"

(generate-sql :postgres {1 :id, 2 :name} {:limit 20})
;; -> "SELECT * FROM data LIMIT 20;"

(generate-sql :sqlserver {1 :id, 2 :name} {:limit 20})
;; -> "SELECT TOP 20 * FROM data;"

## Running the code
This project uses Maven. Maven is used for dependency management and
automation in the project. Check you have Maven installed with:
```aidl
mvn --version
```
To compile the code and run the corresponding unit tests,
run the following command:
```aidl
mvn package
```
Once compilation is done, run the code with the command:
```
java -cp target/metabase-app-1.0-SNAPSHOT.jar com.metabase.app.Main
```

