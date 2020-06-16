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
	private final TECL toplevelTECL = new TECL("$");	
	
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
tableHeaderCol : ID                             	{ tableKeys.add($ID.text); }
               ;
               
tableData : (COMMENT NEWLINE)						// with a table data block only comment lines are allowed, no empty lines				
          | ('|'                                    { tableVals.clear(); }                                   
            (tableDataCol '|')+                     { addTableData(); }
            (COMMENT)? NEWLINE);
tableDataCol : ID                               	{ tableVals.add($ID.text); }
             ;
             
group : ID ((COMMENT)? NEWLINE)* 					{ startGroup($ID.text); }
        '{' ((COMMENT)? NEWLINE)*                    
        configs* 
        '}'                                         { endGroup(); }
        ;          

property : ID val=ASSIGMENT NEWLINE					{ tecl.addProperty($ID.text, ANTLRHelper.me().sanatizeAssignment($val.text)); } 
         ;
         
/*------------------------------------------------------------------
 * LEXER RULES
 *------------------------------------------------------------------*/

ID : [a-zA-Z0-9_]+; 
ASSIGMENT : ':' (~('\r' | '\n'))+;
COMMENT : '#' (~('\r' | '\n'))*;
NEWLINE : ('\r\n' | '\n');
WS: [ \t]+ -> skip;