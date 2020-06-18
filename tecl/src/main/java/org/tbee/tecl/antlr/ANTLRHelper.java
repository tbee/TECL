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
		System.out.println("-----");
		System.out.println("sanatize:"  + s);
		
//		// If the remaining string is blank. it is trimmed to empty
//		if (s.isBlank()) {
//			System.out.println("sanitize done: blank string, becomes empty string");
//			return "";
//		}

		// check to see if it is quoted
		String trimmed = s.trim();
		int trimmedLen = s.length();
		if ( s.length() > 1 
		  && "\"".contentEquals(trimmed.substring(0, 1))
		  && "\"".contentEquals(trimmed.substring(trimmedLen - 1, trimmedLen))
		  ) {
			s = sanatizeQuotedString(s);
		}
		else {
			s = sanatizeUnquotedString(s);
		}
		
		System.out.println("sanatize done:"  + s);
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeQuotedString(String s) {
		System.out.println("sanatize: treat as quoted string" + s);

		// strip quoted
		s = s.substring(1, s.length() - 1);
		System.out.println("sanatize: trimmed quotes"  + s);
		
		// unescape
		s = StringEscapeUtils.unescapeJava(s);
		
		// done
		return s;
	}

	/*
	 * 
	 */
	private String sanatizeUnquotedString(String s) {
		System.out.println("sanatize: treat as unquoted string" + s);

		// done
		return s.trim();
	}
}
