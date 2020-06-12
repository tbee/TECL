grammar TECL;

@header {
	import java.util.Stack;
    import org.tbee.tecl.TECL;
}

@parser::members
{
	public void parse() {
		teclStack.push(tecl);	
		this.configs();
	}
	
	/** */
	public TECL getTECL() {
		return this.tecl;
	}
	private TECL tecl = new TECL();	
	
	private Stack<TECL> teclStack = new Stack<>();
}

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

configs : EOF 
        | config+;

config : group
       | property
       ;

group : ID NEWLINE* '{' NEWLINE*                   { this.teclStack.push( this.teclStack.peek().addGroup($ID.text) ); } 
        configs* 
        '}'                                        { this.teclStack.pop(); }
        ;          

property : ID '=' STRING_LITERAL NEWLINE?          { this.teclStack.peek().addProperty($ID.text, $STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
         | key=ID '=' val=ID NEWLINE?              { this.teclStack.peek().addProperty($key.text, $val.text); }
         ;          

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

STRING_LITERAL : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';
NUM : ('0'..'9')+ ('.' ('0'..'9')+)?;
INT : ('0'..'9')+;
ID : [a-zA-Z_][a-zA-Z0-9_]+; 
NEWLINE : [\r\n] ;
WS: [ \t]+ -> skip;