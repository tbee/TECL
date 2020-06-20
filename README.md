# TECL #

People have complaints about configuration languages all the time; properties are too simple, XML is too verbose, JSON doesn’t allow comments, YAML is only suited for small configuration files, TOML’s tables are a bit weird (IMHO). So how about we throw all the good aspects onto a heap and see what comes out of it? And with only one task in mind: configuration. (Just like JSON is intended for datatransfer, hence it does not support comments.) Therefor I’m introducing TECL, the Totally Encompassing Configuration Language.

The goals are:

- Simple, like properties, but supporting a hierarchy
- Compact, like JSON, but allowing comments
- Schema support
- Formal hierarchy, unlike YAML’s indentation based one
- Conditions
- Multi line strings
- Tables

## Example ##
To get an impression of a TECL configuration file, look at the example below:

```bash
# This is the most simple configuration, if you need spaces or new lines you'll need quotes
title : "TECL rulez"
 
# We use ISO notations only, so no local styles
releaseDateTime : 2020-09-12T06:34
 
# Multiline strings
description : "So,
I'm curious 
where this will end."
 
# Shorcut string; no quotes are needed in a simple property style assignment
# Or if a string is just one word. These strings are trimmed.
protocol : http
 
# Conditions allow for overriding, best match wins (most conditions)
# If multiple condition sets equally match, the first one will win.
title[env=production] : "One config file to rule them all"
title[env=production & os=osx] : "Even on Mac"
 
# Lists
hosts : [alpha, beta]
 
# Hierarchy is implemented using groups denoted by curly brackets
database {
 
    # indenting is allowed and encouraged, but has no semantic meaning
    url : jdbc://...
    user : "admin"
 
    # Strings support default encryption with a external key file, like maven
    password : "FGFGGHDRG#$BRTHT%G%GFGHFH%twercgfg"
 
    # groups can nest
    dialect {
        database : postgres
    }
}
 
servers {
    # This is a table:
    # - the first row is a header, containing the id's
    # - the remaining rows are values
    | name     | datacenter | maxSessions | settings                    |
    | alpha    | A          | 12          |                             |
    | beta     | XYZ        | 24          |                             |
    | "sys 2"  | B          | 6           |                             |
    # you can have sub blocks, which are id-less groups (id is the column)
    | gamma    | C          | 12          | {breaker:true, timeout: 15} |
    # or you reference to another block
    | tango    | D          | 24          | $environment                |
}
 
# environments can be easily done using conditions
environment[env=development] {
    datasource : tst
}
environment[env=production] {
    datesource : prd
}
```

## Usage ##
Equally interesting is of course how this configuration can be accessed:

```java
// Conditions are resolved at parse time.
// So in memory there will be only one version of an id like "environment".
TECL tecl = TECL.parser()
.addCondition("env", "production")
.parse("..filename..");
 
// The initial object will point to the toplevel.
// The method name indicates the return type.
String title = tecl.str("title"); 
LocalDateTime releaseDateTime = tecl.localDateTime("releaseDateTime");
List<String> hosts = tecl.strs("hosts");
 
// You can specify defaults in value methods
String port = tecl.int("port", 80);
 
// The grp method takes it one level / group deeper.
// You simply get a new TECL object for inside that group.
TECL databaseTECL = tecl.grp("database");
 
// So fields are accessed just like in the toplevel
String url = databaseTECL.str("url");
String password = databaseTECL.decrypt("password");
 
// Or directly using method chaining
String url2 = tecl.grp("database").str("url");
 
// Or dot notation?
String url3 = tecl.str("database.url");
 
// And you can move back up
String title2 = databaseTECL.parent().str("title");
 
// In case a group was not defined, get ALWAYS returns an empty TECL.
// This will prevent null pointers in call chains.
// Only a leaf (value) method will return a null if undefined.
// So in the case below, field will be null, but no NPE will be thrown.
String field = tecl.grp("notThere").grp("alsoNotThere").str("field");
 
// Tables use indexes and lists
// Index is the first parameter in order not to confuse with defaults
String ip = tecl.grp("servers").str(0, "ip");
int timeout = tecl.grp("servers").grp(3, "settings").int("timeout");
String datasource = tecl.grp("servers").grp(4, "settings").str("datasource");
 
// You can also index by using one key to search of
int maxSessions = servers.grp("servers").int("name", "gamma", "MaxSessions"); // returns 12
 
// Or use a dot notation with array style index?
String ip2 = tecl.str("servers[0].ip");
```

## Validation ##
And of course you would want to make sure the TECL file matches an agreed upon format, by using a TECL schema:

```bash
version = 1
 
| id              | type          | subtype  | minValues | maxValues |
| title           | string        |          | 1         |           |
| description     | string        |          |           |           |
| releaseDateTime | localDateTime |          |           |           |
| hosts           | list          | string   | 1         | 5         |
| database        | database      |          |           |           |
| servers         | table         | servers  |           | 10        |
| protocol        | protos        |          |           |           |
| protocols       | list          | protos   |           |           |
 
database {
    | id       | type      | minLen | maxLen |
    | url      | string    | 10     | 255    |
    | username | string    |        |        |
    | password | encrypted |        |        |
}
 
servers {
    | id          | type   | min | max |
    | id          | string |     |     |
    | datacenter  | string |     |     |
    | maxSessions | int    | 0   | 50  |
}
 
protos = [http, https]
```