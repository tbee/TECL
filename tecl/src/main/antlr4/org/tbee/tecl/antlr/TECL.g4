grammar TECL;

@parser::header {
	import java.util.Stack;
	import java.util.List;
	import java.util.ArrayList;
    import org.tbee.tecl.TECL;
}

@parser::members
{
	public ParserRuleContext parse() {
		System.out.println("push $");
		teclStack.push(toplevelTECL);
		return this.configs();
	}
	
	// --------------
	// TECL
	
	/** */
	public TECL getTECL() {
		return this.toplevelTECL;
	}
	private final TECL toplevelTECL = new TECL("$");	
	
	private final Stack<TECL> teclStack = new Stack<>();
	private TECL tecl = toplevelTECL;	
	
	// --------------
	// GROUP
	
	private void startGroup(String id) {
		System.out.println("startGroup " + id);
		teclStack.push( teclStack.peek().addGroup(id) ); 
		tecl = teclStack.peek();		
	}
	
	private void endGroup() {
		System.out.println("endGroup " + teclStack.peek().getId());
		teclStack.pop(); 
		tecl = teclStack.peek();		
	}
	
	// --------------
	// TABLE
	
	private final List<String> tableKeys = new ArrayList<>();
	private final List<String> tableVals = new ArrayList<>();
	private int tableRowIdx;
	private final List<TECL> teclsContainingTable = new ArrayList<>();

	private void validateOneTablePerGroup() {
		if (teclsContainingTable.contains(tecl)) {
			throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
		} 
		teclsContainingTable.add(tecl);
	}
		
	private void addTableData() {
		for (int i = 0; i < tableKeys.size(); i++) {
			String key = tableKeys.get(i);
			String val = tableVals.get(i);
			tecl.setProperty(tableRowIdx, key, val);
		}
		tableRowIdx++;
	}
	
	private void startTable() {
		validateOneTablePerGroup();  
		tableKeys.clear(); 
		tableRowIdx = 0;		
	}
}

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

input_file
 : configs EOF
 ;

configs
 : NL* ( config ( NL+ config )* NL* )?
 ;

config
 : property
 | group
 | table
 ;

property
 : WORD conditions? ASSIGN value						{ tecl.addProperty($WORD.text, ANTLRHelper.me().sanatizeAssignment($value.text)); } 
 | WORD conditions? ASSIGN       						{ tecl.addProperty($WORD.text, ""); } 
 ;

group
 : WORD conditions?                                     { startGroup($WORD.text); } 
   NL* OBRACE configs CBRACE                            { endGroup(); }
 ;

conditions
 : OBRACK condition ( AND condition )* CBRACK
 ;

condition
 : WORD EQUALS value						 
 ;

table
 : row ( NL+ row )*
 ;

row
 : PIPE ( col_value PIPE )+
 ;

col_value
 : ~( PIPE | NL )*
 ;

value
 : WORD
 | VARIABLE
 | string
 | list
 ;

string
 : STRING
 | WORD+
 ;

list
 : OBRACK ( value ( COMMA value )* )? CBRACK
 ;

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

ASSIGN : ':';
OBRACK : '[';
CBRACK : ']';
OBRACE : '{';
CBRACE : '}';
COMMA  : ',';
PIPE   : '|';
AND    : '&';
EQUALS : '=';

VARIABLE
 : '$' WORD
 ;

NL
 : [\r\n]+
 ;

STRING
 : '"' ( ~[\\"] | '\\' . )* '"'
 ;

WORD
 : ~[ \t\r\n[\]{}:=,|&]+
 ;

COMMENT
 : '#' ~[\r\n]* -> skip
 ;

SPACES
 : [ \t]+ -> skip
 ;
