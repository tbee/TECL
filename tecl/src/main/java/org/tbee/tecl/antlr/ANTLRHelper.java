package org.tbee.tecl.antlr;

import org.apache.commons.text.StringEscapeUtils;

class ANTLRHelper {

	// for easy stubbing in unittesting
	static ANTLRHelper me = new ANTLRHelper();
	static public ANTLRHelper me() {
		return me;
	}
	
	/**
	 * We have a number of notations
	 * - '=' 
	 * - '= some free test until the end of line' 
	 * - '= some free test until the # comment' 
	 * - '= "quoted # string"' 
	 * - '= "quoted # string with " # comment' 
	 * 
	 * @param s
	 * @return
	 */
	String sanatizeAssignment(String s) {
		//System.out.println("-----");
		//System.out.println("sanatize:"  + s);
		
		// Strip the assignment operator
		s = s.substring(1);
		//System.out.println("sanatize: remove assignment operator:"  + s);
		
		// If the remaining string is blank. it is trimmed to empty
		if (s.isBlank()) {
			//System.out.println("sanitize done: blank string, becomes empty string");
			return "";
		}
		
		// If the first non blank character is a quote, we assume quoted string.
		// We know it is not blank, so at least one character is there.
		if ("\"".equals(s.stripLeading().substring(0, 1))) {
			s = sanatizeQuotedString(s);
		}
		else {
			s = sanatizeUnquotedString(s);
		}
		
		//System.out.println("sanatize done:"  + s);
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeQuotedString(String s) {
		//System.out.println("sanatize: treat as quoted string" + s);

		// Find the position of the last quote
		int lastQuoteIdx = s.lastIndexOf("\"");
		//System.out.println("sanatize: lastQuoteIdx="  + lastQuoteIdx);
		if (lastQuoteIdx == 0) {
			return sanatizeUnquotedString(s);
		}
		
		// Find the last occurrence of the comment marker
		int commentMarkerIdx = s.lastIndexOf("#");
		//System.out.println("sanatize: commentMarkerIdx="  + commentMarkerIdx);
		if (commentMarkerIdx >= lastQuoteIdx) {
			s = s.substring(0, commentMarkerIdx );
			//System.out.println("sanatize: stripped comment: "  + s);
		}
		
		// Trim the remaining string
		s = s.trim();
		// The last quote must be the last character now
		if (lastQuoteIdx != s.length()) {
			throw new IllegalStateException("Extra text after last quote: "  + s);
		}
		
		// Strip first and last quote
		//System.out.println("sanatize: stripped quotes: "  + s);
		s = s.substring(1, s.length() - 1);
		
		// unescape
		s = StringEscapeUtils.unescapeJava(s);
		
		// done
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeUnquotedString(String s) {
		//System.out.println("sanatize: treat as unquoted string" + s);
		
		// Find the last occurrence of the comment marker
		int commentMarkerIdx = s.lastIndexOf("#");
		//System.out.println("sanatize: commentMarkerIdx="  + commentMarkerIdx);
		if (commentMarkerIdx >= 0) {
			s = s.substring(0, commentMarkerIdx);
			//System.out.println("sanatize: stripped comment: "  + s);
		}
		
		// done
		return s.trim();
	}
}
