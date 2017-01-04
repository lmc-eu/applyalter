
ApplyAlter - tool for applying alterscripts
==========

This tool allows easy and robust deployment process - both manual (releases handed to OPS) and automated (even continuous deployment, to limited extent). Unlike some more radical approaches, applyalter is designed for database-first design and fully supports data changes and migrations (ie not just DDL, but also DML).

Main advantages over ".sql" scripts are:
* execute single file, zip archive or whole directory
* commands are executed in transaction (by default)
* in-build checks to skip creating already existing entities (tables, columns etc.)
* custom checks to skip changing already changed data
* each executed alterscript is logged to log table (with execution time, duration and checksum)
* fine-grained error control - by default every error is fatal, but some of them can be ignored for specific commands
* support for characters in any encoding (as long as it's well-formed XML)
* dry-run mode
* database identification in script metadata
* attached files (BLOB, CLOB or CSV parsed into columns)
* environment-only scripts
* special support for large-scale data changes that cannot fit into single transaction
* not tied to one specific database (used on db2, postgresql and oracle)

How to build and run
--------------------

1. Download sources / clone repository.
2. Build jar package by maven:
  + `maven package`
  + If you want to have some JDBC driver embedded, simply activate one of the embed_XXXX profile (for example `-Pembed_postgresql_8`).
    See `pom.xml` content for details. If you need different version, just add a new profile (and please contribute via pull request).
  + Note that there are several environment variables used to store build data (`BUILD_ID`, `BUILD_URL`, `GIT_BRANCH`, `GIT_COMMIT`).
    These variables are automatically provided by Hudson or Jenkins build server, but they are completely optional.
3. Check that the jar file is successfuly built and it is runnable:
    ```
    java -jar target/applyalter-*.jar
    ```
    Basic usage and version info should be displayed.
4. When running for real, do not forget that non-embedded jdbc drivers must be present beside the executable jar
   and their names must be in the manifest classpath. Manifest classpath is, by default:
   `db2jcc.jar db2jcc4.jar postgresql.jar ojdbc14.jar ojdbc5.jar`

Database configuration
----------------------
First parameter is *always* confgiuration file that describes how to connect to database(s).
The support of multiple database might be a little confusing here: all database in single configuration file
are supposed to form "cluster" and have the same schema except minor difference. This feature is *not*
intended to be used as a way to store all databases in single global configuration file!

Basic format is quite simple: the root element `db` contains list of database instances:
```
<db>
  <pginstance>
    <id>brand0</id>
    <type>aden</type>
    <host>localhost</host>
    <!-- <port>5432</port> -->
    <db>brand0</db>
    <user>podlesh</user>
    <pass>some_password</pass>
  </pginstance>
</db>
```

Name of the database instance element defines database type, used driver and supported options:

| element | driver | |
| ---------- | ----------------------- | ------------------- |
| pginstance | `org.postgresql.Driver` | when `<pass>` is missing or empty, `$HOME/.pgpass` content is used  |
| dbinstance | `com.ibm.db2.jcc.DB2Driver` | remote connection, requires all options including `port` |
| db2native | `com.ibm.db2.jcc.DB2Driver` | local native connection, uses *only*  `db` element |
| oracle-instance | `oracle.jdbc.driver.OracleDriver` | Oracle support is only rudimentary. | 

Each database instance *must* contain element `id` with unique identifier of that instance; this identifier
must be unique inside single configuration file, but not globally.
This value is also used instead of `type` when that element is missing; this is convenient for the most common 
case of single database per config file.

| element | description |
| ------ | ------------------- |
| `type` | custom identifier of database type or application ; only used to filter alterscripts by matching apropriate element in them |
| `host` | required by all except `db2native` |
| `port` | required by `dbinstance`, optional for `pginstance` and `oracle-instance` |
| `db` | database name used by DBMS; **always required** |
| `user` | username; required by `dbinstance`, optional for `pginstance` when `$HOME/.pgpass` is present and contains match |
| `pass` | password; required by `dbinstance`, optional for `pginstance` when `$HOME/.pgpass` is present and contains match |


Alterscript
-----------
Each alterscript is single XML file containing two main sections: *metadata* and *commands*. Metadata
describe where could the alterscript be executed (it should match the configuration file content, otherwise error is reported)
and provide some advanced features. Commands are then executed.

*Whenever possible, each alterscript is executed in single transaction, with rollback on error. On success, record about
execution is stored to special table `APPLYALTER_LOG` .* 

Package log table and queries
-----------------------------
All alterscripts executed in single invocation (ie all commandline arguments)
are considered single "*package*" and their complete checksum is recorded to
special table `applyalter_pkg`. Unlike `applyalter_log` (which records single
alterscripts), this one is not checked in any way and used only to query information
about past invocations. Main use case is the "check_mode" of ansible module.

To query this table and list packages (invocations), specify option `--query-pkg`
with path to output file. After standard execution, all records with the same
SHA1 checksums and the same database ID are found and written as XML file.
* Query result also includes this very invocation, as long as it was really executed
  (ie SHARP mode, which is default).
* If there are no alterscripts to load (ie the only parameter is configuration file),
  complete history is dumped for this database.
* Explicit checksum to query can be specified by option `--query-pkg-hash`
