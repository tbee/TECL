package org.tbee.tecl.antlr;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class PrintLexer implements org.tbee.tecl.antlr.TECLParser.Listener {
	private static void printPrettyLispTree(String tree, StringBuilder sb) {
		int indentation = 1;
		for (char c : tree.toCharArray()) {
			if (c == '(') {
				if (indentation > 1) {
					sb.append("\n");
				}
				for (int i = 0; i < indentation; i++) {
					sb.append("  ");
				}
				indentation++;
			} else if (c == ')') {
				indentation--;
			}
			sb.append(c);
		}
		sb.append("\n");
	}

	public String lex(String source) {
		StringBuilder sb = new StringBuilder();
		lex(CharStreams.fromString(source), sb);
		return sb.toString();
	}
	
	private void lex(CharStream input, StringBuilder sb) {
		TECLLexer lexer = new TECLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        sb.append("\n[TOKENS]\n");
        for (Token t : tokens.getTokens()) {
            String symbolicName = TECLLexer.VOCABULARY.getSymbolicName(t.getType());
            String literalName = TECLLexer.VOCABULARY.getLiteralName(t.getType());
            sb.append(String.format("  %-20s '%s'\n",
                    symbolicName == null ? literalName : symbolicName,
                    t.getText().replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")));
        }
        sb.append("\n[PARSE-TREE]\n");
        TECLParser parser = new TECLParser(tokens);
        ParserRuleContext context = parser.parse(this);
        String tree = context.toStringTree(parser);
        printPrettyLispTree(tree, sb);
    }

	@Override
	public void setProperty(int idx, String key, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startGroup(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endGroup() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startConditions() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addCondition(String key, String comparator, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void terminateTable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startTableRow() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableData(String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProperty(String key, List<String> values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTableData(List<String> value) {
		// TODO Auto-generated method stub
		
	}
}
