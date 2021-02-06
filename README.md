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

# Single quoted strings will have all the spaces of indentation, including preceding and trailing whitespace & newlines.
# Triple quoted strings will be stripped of the preceding and trailing whitespace & newlines, and the indentation of the lines themselves.
# Triple quoted strings will remove as much indent as the minimal NUMBER of whitespaces used to indent any of the lines; 
#   so if one line used 2 tabs and another 8 spaces to indent, both will get 2 whitespace removed, leaving the other line with 6 space remaining.
indented {
	description1: 	" 
					All this whitespace will be in the string.
					Including the spaces + newline after the first quote and after this line until the end quote. 
					"
	description2: 	"""    
					Every newline and whitespace before the first letter of this line will be trimmed.
					And every newline and whitespace after the end of this line as well.
					"""
} 

# Shorcut string; no quotes are needed if the value is just one word.
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
The configuration hierarchy is accessed using a single 'list' method, because each key may have multiple values:  

	List<R> list(String path, List<R> defaultValue, Class<R> clazz)

But if you only want one (which is very often the case) there is the 'get':  

	R get(String path, R defaultValue, Class<R> clazz)

The class at the end specifies the return type. 
TECL has build in support for Strings, Integers, BigDecimal, BigInteger, date, time and more will follow.
These build in types also have convenience methods:

	str("/database/url")
	integer("/database/timepout")
	grp("/database")

But in case no matching convenience method is present, the get or list method is the way to go.
It is possible to register custom types (see below), or simply provide a convert function to the 'listUsingFunction' method.

The path in the methods travels the hierarchy.
If no value is found, the default is returned. Per default this is null, but can be set as the last parameter:

	integer("/database/timepout", 1000)

Every property can have multiple values, to get to those indexes are used as the first parameter: 

	str(1, "/hosts")
	str("/hosts[1]")

Tables are nothing more than indexed properties:

    integer(4, "/servers/maxSessions")
    integer("/servers/maxSessions[4]")
    
And the same principle is used for groups:

    grp(1, "/environment")
    grp("/environment[1]")
    
There is one noticeable difference between properties and groups: a 'grp' call will never return null, even if the group is not present. 
If a group does not exist, TECL will create an empty group and return that, to prevent null point exceptions. 
Nulls can only be returned at the leaf or value nodes. 

So the call below does not result in a null pointer exception because there are missing groups, but simply returns null for the fact that 'key' does not exist in the last group:

	str("/the/groups/in/the/path/do/not/exist/key")
	

### Example ###

The example configuration above can be accessed in the following way:

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
String port = tecl.integer("port", 80);
 
// The grp method takes it one level / group deeper.
// You simply get a new TECL object for inside that group.
TECL databaseTECL = tecl.grp("database");
 
// So fields are accessed just like in the toplevel
String url = databaseTECL.str("url");
String password = databaseTECL.decrypt("password");
 
// Or directly using a path
String url2 = tecl.str("database/url");
 
// The path starts at the current node, or root if it starts with a slash
String url3 = tecl.str("/database/url");
 
// And you can move back up
String title2 = databaseTECL.str("../title");
 
// In case a group was not defined, an empty TECL is ALWAYS returned.
// This will prevent null pointers in call chains.
// Only a leaf (value) method will return a null if undefined.
// So in the case below, field will be null, but no NPE will be thrown.
String field = tecl.str("/notThere/alsoNotThere/field");
 
// Tables are accessed using indexes/
// Index is the first parameter in order not to confuse with defaults
String ip = tecl.str(0, "/servers/ip");
// But indexes can also be written in the path
String ip2 = tecl.str("/servers/ip[0]");
int timeout = tecl.integer("/servers/settings[3]/timeout");
 
// Practical for tables is a lookup function: search for the row where name=gamma and get the value for maxSessions
int maxSessions = tecl.integer("/servers/name", "gamma", "/server/maxSessions"); // returns 12
// For readability it is better to first scope on the group
int maxSessions2 = tecl.grp("/servers").integer("name", "gamma", "maxSessions"); // returns 12
```

## Validation ##
The user basically determines at runtime how a property is to be interpreted, calling `bd("key")` will make the value being parsed as a BigDecimal.
So it is only at runtime that you know if a value can be parsed as a double. 
This is quite normal for configuration files, but TECL tries to improve this by supporting a schema.
In the schema you can specify the type, frequency and other characteristics of properties and groups.


```bash
@version = 1
 
| id              | type          | subtype  | enum   |minValues | maxValues |
| title           | String        |          |        | 1         |           |
| description     | String        |          |        |           |           |
| releaseDateTime | LocalDateTime |          |        |           |           |
| hosts           | list          | String   |        | 1         | 5         |
| database        | group         | database |        |           |           |
| servers         | group         | servers  |        |           | 10        |
| protocols       | list          | String   | protos |           |           |
| protos          | list          | String   |        |           |           |
 
database {
    | id       | type      | minLen | maxLen |
    | url      | string    | 10     | 255    |
    | username | string    |        |        |
    | password | encrypted |        |        |
}
 
servers {
    | id          | type    | min | max |
    | name        | String  |     |     |
    | datacenter  | String  |     |     |
    | maxSessions | Integer | 0   | 50  |
}
 
protos = [http, https]
```

The schema can be specified just before parsing:

```java
TECL tecl = TECL.parser()
    .schema("..schemafile.." ) // Optionally custom validators can be added here
    .parse("..filename..");
```

## References ##
In order to prevent very complex and deeply nested data, TECL allows for references.
This are written like the paths when accessing the data (which is similar to xpath expressions), starting with a $-sign. For example:


```bash
key1 : $key2
key2 : value
```

References are handled transparently:

```java
parse.str("key1") // returns "value"
```

This of course becomes more relevant as a TECL becomes more complex. Fetching the value of '/server/name' in the TECL below will return "value".

```bash
servers {
    | name  | 
    | $/key | 
}
key : value
```

Note the slash that was inserted in the reference. This is needed because '$key' would have been solved within the 'servers' group, and thus be null. The slash makes it refer to the root-level.

It is also possible to use indexes in references, like so:

```bash
key : $/servers/name[1]
servers {
    | name   | 
    | value1 | 
    | value2 | 
}
```

Or refer to the parent:

```bash
key : $/group1/group2/name
group1 {
    group2 {
        | name     | 
        | $../key2 | 
    }
    key : value
}
```

## Custom convert functions and types ##
TECL supports build in types for a number of types, but you can easily register your own:

```java
// This is the new type
class Temperature {
	int value;
	String unit;
}

// Create the parser where the type can be registered
TECLParser parser = TECL.parser();

// Add the new type by providing a converted function. 
// The parameters are the STRing that needs to be converted, and the DEFault that may be returned when something goes wrong.
parser.addConvertFunction(Temperature.class, (str, def) -> {
	Temperature t = new Temperature();
	t.value = Integer.parseInt(str.replace("F", ""));
	t.unit = str.substring(str.length() - 1);
	return t;
});

// Now parse a file
TECL tecl = parser.parse("key : 123F \n");

// And get the key as the registered new type.
Temperature temperature = tecl.get("key", Temperature.class);
List<Temperature> temperatures = tecl.list("key", Temperature.class);
```

Or just provide a convert function:

```java
TECL tecl = TECL.parser().parse("...");
List<Integer> ints = tecl.listUsingFunction("key", Integer::parseInt);
```

## Command line arguments ##
TECL supports adding the command line arguments as values, so it is possible to override a configuration file.

```java
static public void main(String[] args) {
	TECL tecl = TECL.parser().parse(MyClass.getResourceAsStream("config.tecl"));
	tecl.addCommandLineArguments(args);
}
```

You can then call the application with arguments like:

```bash
java -jar app.jar -key value
java -jar app.jar -/group/key value
```

In order to set multiple values, a key needs to be repeated:

```bash
java -jar app.jar -key value1 -key value2
```