package org.tbee.tecl.antlr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class PrintLexer {
	private static void printPrettyLispTree(String tree) {
		int indentation = 1;
		for (char c : tree.toCharArray()) {
			if (c == '(') {
				if (indentation > 1) {
					System.out.println();
				}
				for (int i = 0; i < indentation; i++) {
					System.out.print("  ");
				}
				indentation++;
			} else if (c == ')') {
				indentation--;
			}
			System.out.print(c);
		}
		System.out.println();
	}

	public void lex(String source) {
		lex(CharStreams.fromString(source));
	}
	
	private void lex(CharStream input) {
		TECLLexer lexer = new TECLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        System.out.println("\n[TOKENS]");
        for (Token t : tokens.getTokens()) {
            String symbolicName = TECLLexer.VOCABULARY.getSymbolicName(t.getType());
            String literalName = TECLLexer.VOCABULARY.getLiteralName(t.getType());
            System.out.printf("  %-20s '%s'\n",
                    symbolicName == null ? literalName : symbolicName,
                    t.getText().replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"));
        }
        System.out.println("\n[PARSE-TREE]");
        TECLParser parser = new TECLParser(tokens);
        ParserRuleContext context = parser.input_file();
        String tree = context.toStringTree(parser);
        printPrettyLispTree(tree);
    }
}
