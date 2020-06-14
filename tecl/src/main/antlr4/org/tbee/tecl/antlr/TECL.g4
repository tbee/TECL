grammar TECL;

@parser::header {
	import java.util.Stack;
	import java.util.List;
	import java.util.ArrayList;
    import org.tbee.tecl.TECL;
}

@parser::members
{
	public void parse() {
		teclStack.push(tecl);	
		this.configs();
	}
	
	// --------------
	// TECL
	
	/** */
	public TECL getTECL() {
		return this.tecl;
	}
	private final TECL tecl = new TECL();	
	
	private final Stack<TECL> teclStack = new Stack<>();
	
	// --------------
	// TABLE
	
	private final List<String> tableKeys = new ArrayList<>();
	private final List<String> tableVals = new ArrayList<>();
	private int tableRowIdx;
	private final List<TECL> teclsContainingTable = new ArrayList<>();
	
	private void addTableData() {
		System.out.println("vals=" + tableVals);
		TECL tecl = teclStack.peek();
		for (int i = 0; i < tableKeys.size(); i++) {
			String key = tableKeys.get(i);
			String val = tableVals.get(i);
			tecl.addIndexedProperty(tableRowIdx, key, val);
		}
		tableRowIdx++;
	}
}

/*------------------------------------------------------------------
 * PARSER RULES
 *------------------------------------------------------------------*/

configs : EOF 
        | config+;

config : group
       | table
       | property
       | NEWLINE
       ;

table : tableHeader tableData+;

tableHeader : '|'                                   {
														TECL tecl = teclStack.peek();
														if (teclsContainingTable.contains(tecl)) {
															throw new IllegalStateException("Group " + tecl.getId() + " already contains a table, only one table per group is allowed.");
														} 
														teclsContainingTable.add(tecl);
														tableKeys.clear(); 
														tableRowIdx = 0;
													} 
              (tableHeaderCol '|')+                { System.out.println("keys=" + this.tableKeys); }
              NEWLINE;
tableHeaderCol : ID                                { tableKeys.add($ID.text); }
               ;
               
tableData : '|'                                    { tableVals.clear(); }                                   
            (tableDataCol '|')+                    { addTableData(); }
            NEWLINE;
tableDataCol : ID                                  { tableVals.add($ID.text); }
             | STRING_LITERAL                      { tableVals.add($STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
             ;
             
group : ID NEWLINE* '{' NEWLINE*                   { teclStack.push( this.teclStack.peek().addGroup($ID.text) ); } 
        configs* 
        '}'                                        { teclStack.pop(); }
        ;          

property : ID '=' STRING_LITERAL NEWLINE?          { teclStack.peek().addProperty($ID.text, $STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
         | key=ID '=' val=ID NEWLINE?              { teclStack.peek().addProperty($key.text, $val.text); }
         ;          

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

STRING_LITERAL : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';
ID : [a-zA-Z0-9_]+; 
NEWLINE : ('#' .*)? '\r\n' | '\n';
WS: [ \t]+ -> skip;