grammar TECL;

@header {
  import org.tbee.tecl.TECL;
}

@members
{
	/** */
	public TECL getTECL() {
		return this.tecl;
	}
	private TECL tecl = new TECL();	
} 

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

configs : EOF 
        | config+;

config : property
       ;

property : ID '=' STRING_LITERAL NEWLINE?          { this.tecl.addProperty($ID.text, $STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
         | key=ID '=' val=ID NEWLINE?              { this.tecl.addProperty($key.text, $val.text); }
         ;          

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

STRING_LITERAL : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';
NUM : ('0'..'9')+ ('.' ('0'..'9')+)?;
INT : ('0'..'9')+;
ID : ('a'..'z'|'_')('a'..'z'|'0'..'9'|'_')* ;
NEWLINE : [\r\n] ;
WS: [ \t]+ -> skip;