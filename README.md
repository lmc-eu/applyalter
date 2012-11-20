
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

