grammar TECL;

@parser::header {
	import java.util.Stack;
	import java.util.List;
	import java.util.ArrayList;
    import org.tbee.tecl.TECL;
}

@parser::members
{
	static public interface Listener {
		void addProperty(String key, String value);
		void addProperties(String key, List<String> values);
		void setProperty(int idx, String key, String value);
		
		void startGroup(String id);
		void endGroup();
		
		void startConditions();		
		void addCondition(String key, String comparator, String value);
		
		void startTable();
		void terminateTable();
		void startTableRow();
		void addTableData(String value);
	}
	
	public ParserRuleContext parse(Listener listener) {
		this.listener = listener;
		return this.configs();	
	}
	private Listener listener;
	
	private List<String> listValues = new ArrayList<>();
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
 : WORD conditions? ASSIGN value						{ listener.setProperty(0, $WORD.text, $value.text); } 
 | WORD conditions? ASSIGN list							{ listener.addProperties($WORD.text, listValues); } 
 | WORD conditions? ASSIGN       						{ listener.setProperty(0, $WORD.text, ""); } 
 ;

group
 : WORD conditions?                                     { listener.startGroup($WORD.text); } 
   NL* OBRACE configs CBRACE                            { listener.endGroup(); }
 ;

conditions
 : OBRACK												{ listener.startConditions(); } 
   condition ( AND condition )* CBRACK
 ;

condition
 : WORD COMPAR value						 			{ listener.addCondition($WORD.text, $COMPAR.text, $value.text); }
 ;

table
 :														{ listener.startTable(); } 
 row 
 ( 
	(
		COMMENT? NL										{ if ($NL.text.length() > 1) { listener.terminateTable(); } }		
	)+
 	row
 )*
 ;

row
 :														{ listener.startTableRow(); } 
 PIPE ( 
 	col_value											{ listener.addTableData($col_value.text); } 
 	PIPE
 )+		
 ;

col_value : ~( PIPE | NL )*;

value
 : WORD
 | VARIABLE
 | string
 ;

string
 : STRING
 | WORD+
 ;

list
 : OBRACK ( value 										{ listValues.clear(); listValues.add($value.text); }									
 	( COMMA value 										{ listValues.add($value.text); }
 	)*
 )? CBRACK
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
