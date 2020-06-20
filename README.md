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
