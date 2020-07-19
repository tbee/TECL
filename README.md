# TECL #

People have complaints about configuration languages all the time; properties are too simple, XML is too verbose, JSON doesn’t allow comments, YAML is only suited for small configuration files, TOML’s tables are a bit weird (IMHO). So how about we throw all the good aspects onto a heap and see what comes out of it? And with only one task in mind: configuration. (Just like JSON is intended for datatransfer, hence it does not support comments.) Therefore I’m introducing TECL, the Totally Encompassing Configuration Language.

The goals are:

- Simple, like properties, but supporting a hierarchy
- Compact, like JSON, but allowing comments
- Formal hierarchy, unlike YAML’s indentation based one
- Conditions
- Multi line strings
- Tables
- Schema support

## Example ##
To get an impression of a TECL configuration file, look at the example below:

```bash
# you can specify the version of the TECL file (for future use)
@version 1

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
    timeout: 10
 
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
    # lists are allowed in a table
    | many     | [D,E,F]    |             |                             |
    # you can have sub groups, by referencing another group
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
The configuration hierarchy is accessed using a single 'get' method:  

	List<R> get(String path, List<R> defaultValue, Function<String, R> convertFunction)

If you provide a convertFunction get returns properties, if the convert function is null get returns groups.
The path in the get method travels the hierarchy, so if I want to get the url of the database in the example below: 

	get("/database/url", null, (s) -> s)
	
Properties are stored as strings, so the convertFunction in this case keeps the string as-is (input is string, output is the same string).
An integer would need a other convert function:

	get("/database/timeout", null, Integer::valueOf)

Not complex necessarily, but also not user friendly, so TECL offers convenience methods to make this easier: 

	str("/database/url")
	integer("/database/timepout")

And if you want to access a group:

	get("/database", null, null)
	grp("/database")

There is one noticeable difference between these two calls though: 'grp' will never return null. 
If a group does not exist, it will create a empty group and return that, this is to prevent null point exceptions. 
All attempts to get a value from these groups will return null, but that only can happen at the leaf nodes. 
So the call below does not result in a null pointer exception, because there are missing groups, but simply returns null for 'key' not existing.

	str("/the/groups/in/the/path/do/not/exist/key")
	

### Example ###

So the example configuration above can be accessed in the following way:

```java
// Conditions are resolved at parse time.
// So in memory there will be only one version of an id like "environment".
TECL tecl = TECL.parser()
    .addParameter("env", "production") // to use in conditions
    .parse("..filename..");
 
// The initial object will point to the toplevel TECL, also called root.
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
 
// Or directly using a path chaining
String url2 = tecl.str("database/url");
 
// The path starts at the current node, or root if it starts with a slash
String url3 = tecl.str("/database/url");
 
// And you can move back up
String title2 = databaseTECL.str("../title");
 
// In case a group was not defined, get ALWAYS returns an empty TECL.
// This will prevent null pointers in call chains.
// Only a leaf (value) method will return a null if undefined.
// So in the case below, field will be null, but no NPE will be thrown.
String field = tecl.str("/notThere/alsoNotThere/field");
 
// Tables use indexes and lists
// Index is the first parameter in order not to confuse with defaults
String ip = tecl.str(0, "/servers/ip");
int timeout = tecl.int("/servers/settings[3]/timeout");
String datasource = tecl.str("/servers/settings[4]/datasource");
 
// You can also index by using one key to search of
int maxSessions = servers.grp("servers").int("name", "gamma", "MaxSessions"); // returns 12
```

## Validation (not implemented yet) ##
The user basically determines at runtime how a property is to be interpreted, calling `dbl("key")` will make the value being parsed as a double.
So it is only at runtime that you know if a value can be parsed as a double. 
This is quite normal for configuration files, but TECL tries to improve this by supporting a schema.
In the schema you can specify the type, frequence and other characteristics of properties and groups.


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

## Tables ##
TODO

## Variables ##
TODO
