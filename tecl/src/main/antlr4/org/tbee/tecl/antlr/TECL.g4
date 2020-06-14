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
		teclStack.push(toplevelTECL);
		this.configs();
	}
	
	// --------------
	// TECL
	
	/** */
	public TECL getTECL() {
		return this.toplevelTECL;
	}
	private final TECL toplevelTECL = new TECL();	
	
	private final Stack<TECL> teclStack = new Stack<>();
	private TECL tecl = toplevelTECL;	
	
	// --------------
	// GROUP
	
	private void startGroup(String id) {
		teclStack.push( teclStack.peek().addGroup(id) ); 
		tecl = teclStack.peek();		
	}
	
	private void endGroup() {
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
		System.out.println("vals=" + tableVals);
		for (int i = 0; i < tableKeys.size(); i++) {
			String key = tableKeys.get(i);
			String val = tableVals.get(i);
			tecl.addIndexedProperty(tableRowIdx, key, val);
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

configs : EOF 
        | config+;

config : group
       | table
       | property
       | COMMENT
       | NEWLINE
       ;

table : tableHeader tableData*;

tableHeader : '|'                                   { startTable(); } 
              (tableHeaderCol '|')+                 
              (COMMENT)? NEWLINE;
tableHeaderCol : STRING                             { tableKeys.add($STRING.text); }
               ;
               
tableData : (COMMENT NEWLINE)						
          | ('|'                                    { tableVals.clear(); }                                   
            (tableDataCol '|')+                     { addTableData(); }
            (COMMENT)? NEWLINE);
tableDataCol : STRING                               { tableVals.add($STRING.text); }
             | STRING_LITERAL                       { tableVals.add($STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
             ;
             
group : STRING ((COMMENT)? NEWLINE)* '{' ((COMMENT)? NEWLINE)*                { startGroup($STRING.text); } 
        configs* 
        '}'                                         { endGroup(); }
        ;          

property : STRING '=' STRING_LITERAL        { tecl.addProperty($STRING.text, $STRING_LITERAL.text.substring(1, $STRING_LITERAL.text.length() - 1)); }
         | key=STRING '=' val=STRING        { tecl.addProperty($key.text, $val.text); }
         ;          

/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

STRING_LITERAL : '"' (~('"' | '\\' | '\r' | '\n') | '\\' ('"' | '\\'))* '"';
STRING : [a-zA-Z0-9_]+; 
COMMENT : '#' (~('\r' | '\n'))*;
NEWLINE : ('\r\n' | '\n');
WS: [ \t]+ -> skip;