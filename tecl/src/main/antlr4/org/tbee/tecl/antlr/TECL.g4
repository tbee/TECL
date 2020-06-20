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
		System.out.println("startGroup $");
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
	private int tableColIdx;
	private final List<TECL> teclsContainingTable = new ArrayList<>();
	private final List<TECL> teclsWithTerminatedTable = new ArrayList<>();

	private void validateOneTablePerGroup() {
		if (teclsContainingTable.contains(tecl)) {
			throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
		} 
		teclsContainingTable.add(tecl);
	}
	private void validateTerminatedTable() {
		if (teclsWithTerminatedTable.contains(tecl)) {
			throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
		} 
	}
	private void terminateTable() {
		System.out.println("terminateTable");
		teclsWithTerminatedTable.add(tecl);
	}	
		
	private void startTable() {
		System.out.println("startTable");
		validateOneTablePerGroup();  
		tableKeys.clear(); 
		tableRowIdx = -2;		
	}
	
	private void startTableRow() {
		tableColIdx = 0;
		tableRowIdx++;
		System.out.println("startTableRow row=" + tableRowIdx + ", col=" + tableColIdx);
	}	

	private void addTableData(String value) {
		validateTerminatedTable();
		System.out.println("addTableRow row=" + tableRowIdx + ", col=" + tableColIdx + ", value=" + value);
		if (tableRowIdx < 0) {
			System.out.println("addTableRow add header " + value);
			tableKeys.add(value);
		}
		else {
			String key = tableKeys.get(tableColIdx);
			System.out.println("addTableRow add data " + key + "[" + tableRowIdx + "]=" + value);
			tecl.setProperty(tableRowIdx, key, value);
		}
		tableColIdx++;
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
 : WORD COMPAR value						 
 ;

table
 :														{ startTable(); } 
 row 
 ( 
	(
		COMMENT? NL										{ if ($NL.text.length() > 1) { terminateTable(); } }		
	)+
 	row
 )*
 ;

row
 :														{ startTableRow(); } 
 PIPE ( 
 	col_value											{ addTableData($col_value.text); } 
 	PIPE
 )+		
 ;

col_value : ~( PIPE | NL )*;

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
COMPAR : '=';

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
 : ~[ \t\r\n[\]{}:=<>!,|&]+
 ;

COMMENT
 : '#' ~[\r\n]* -> skip
 ;

SPACES
 : [ \t]+ -> skip
 ;
