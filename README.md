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
To understand how to use TECL it is important to grasp the basic underlying storage: *everything are indexed values*.

- When parsing a configuration the only active difference TECL makes is between properties and groups, so TECL has indexed properies and and indexed groups.
- A group is a new TECL object, so a configuration consists of a tree of indexed TECL objects, with indexed properties as the leafs.
- A property is always a string, unless the user of TECL tells it to interprete it differenty. In a normal configuration file, like properties, the user would get a string and then pull it through a parser, like Integer.valueOf. With TECL you can simply provide the parse method, or use one of the predefined convenience ones.
- Upon parsing it is possible to set parameters, which can exluced groups or properties.

And that is all there is to it. So basically there are only two relevant methods.

	TECL grp(int idx, String key)
	String str(int indx, String key, String defaultValue)

Observable reader may notice that the `grp` call does have a default value parameter.
This is because groups constitute the tree, so in order to prevent NullPointerExceptions when cascade calling into the tree, if a group is not found an empty TECL object is returned.

All other methods are convenience methods, for example:

`public String str(String key)` 
Get the value at index 0, assuming null as the default value.
    
`Double dbl(String key)`
Get the string at index 0 for the given key, assuming null as the default, and pull that through Double::valueOf to convert the String to Double.    

`<R> R get(int idx, String key, R defaultValue, Function<String, R> convertFunction)`
Get the the string value for key at idx, and if found pass that through the given convertFunction, so it will return a type R. This is how custom types are supported.   

`public String str(String indexOfKey, String indexOfValue, String key, String defaultValue)` 
First do a search for a value on one key, and use that index to fetch the value of another key. Similar to a lookup call in Excel.


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
