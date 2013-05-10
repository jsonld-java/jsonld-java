package com.github.jsonldjava.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URL {

	public String href = "";
	public String protocol = "";
	public String host = "";
	public String auth = "";
	public String user = "";
	public String password = "";
	public String hostname = "";
	public String port = "";
	public String relative = "";
	public String path = "";
	public String directory = "";
	public String file = "";
	public String query = "";
	public String hash = "";
	
	// things not populated by the regex (NOTE: i don't think it matters if these are null or "" to start with)
	public String pathname = null;
	public String normalizedPath = null;
	public String authority = null;
	
	private static Pattern parser = Pattern.compile("^(?:([^:\\/?#]+):)?(?:\\/\\/((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\\/?#]*)(?::(\\d*))?))?((((?:[^?#\\/]*\\/)*)([^?#]*))(?:\\?([^#]*))?(?:#(.*))?)");
	
	public static URL parse(String url) {
		URL rval = new URL();
		rval.href = url;
		
		Matcher matcher = parser.matcher(url);
		if (matcher.matches()) {
			if (matcher.group(1) != null) rval.protocol = matcher.group(1);
			if (matcher.group(2) != null) rval.host = matcher.group(2);
			if (matcher.group(3) != null) rval.auth = matcher.group(3);
			if (matcher.group(4) != null) rval.user = matcher.group(4);
			if (matcher.group(5) != null) rval.password = matcher.group(5);
			if (matcher.group(6) != null) rval.hostname = matcher.group(6);
			if (matcher.group(7) != null) rval.port = matcher.group(7);
			if (matcher.group(8) != null) rval.relative = matcher.group(8);
			if (matcher.group(9) != null) rval.path = matcher.group(9);
			if (matcher.group(10) != null) rval.directory = matcher.group(10);
			if (matcher.group(11) != null) rval.file = matcher.group(11);
			if (matcher.group(12) != null) rval.query = matcher.group(12);
			if (matcher.group(13) != null) rval.hash = matcher.group(13);
			
			// normalize to node.js API
			if (!"".equals(rval.host) && "".equals(rval.path)) {
				rval.path = "/";
			}
			rval.pathname = rval.path;
			parseAuthority(rval);
			rval.normalizedPath = removeDotSegments(rval.pathname, !"".equals(rval.authority));
			if (!"".equals(rval.query)) {
				rval.path += "?" + rval.query;
			}
			if (!"".equals(rval.protocol)) {
				rval.protocol += ":";
			}
			if (!"".equals(rval.hash)) {
				rval.hash = "#" + rval.hash;
			}
			return rval;
		}
		
		return rval;
	}

	/**
     * Removes dot segments from a URL path.
     *
     * @param path the path to remove dot segments from.
     * @param hasAuthority true if the URL has an authority, false if not.
     */
	public static String removeDotSegments(String path, boolean hasAuthority) {
		String rval = "";
		
		if (path.indexOf("/") == 0) {
			rval = "/";
		}
		
		// RFC 3986 5.2.4 (reworked)
		List<String> input = new ArrayList<String>(Arrays.asList(path.split("/")));
		if (path.endsWith("/")) {
			// javascript .split includes a blank entry if the string ends with the delimiter, java .split does not so we need to add it manually
			input.add("");
		}
		List<String> output = new ArrayList<String>();
		for (int i = 0 ; i < input.size() ; i++) {
			if (".".equals(input.get(i)) || ("".equals(input.get(i)) && input.size()-i > 1)) {
				//input.remove(0);
				continue;
			}
			if ("..".equals(input.get(i))) {
				//input.remove(0);
				if (hasAuthority || (output.size() > 0 && !"..".equals(output.get(output.size() - 1)))) {
					// [].pop() doesn't fail, to replicate this we need to check that there is something to remove
					if (output.size() > 0) {
						output.remove(output.size() - 1);
					}
				} else {
					output.add("..");
				}
				continue;
			}
			output.add(input.get(i));
			//input.remove(0);
		}
		
		if (output.size() > 0) {
			rval += output.get(0);
			for (int i = 1; i < output.size(); i++) {
				rval += "/" + output.get(i);
			}
		}
		return rval;
	}

	/**
	 * Parses the authority for the pre-parsed given URL.
	 *
	 * @param parsed the pre-parsed URL.
	 */
	private static void parseAuthority(URL parsed) {
		// parse authority for unparsed relative network-path reference
		if (parsed.href.indexOf(":") == -1 && parsed.href.indexOf("//") == 0 && "".equals(parsed.host)) {
			// must parse authority from pathname
			parsed.pathname = parsed.pathname.substring(2);
			int idx = parsed.pathname.indexOf("/");
			if (idx == -1) {
				parsed.authority = parsed.pathname;
				parsed.pathname = "";
			} else {
				parsed.authority = parsed.pathname.substring(0, idx);
				parsed.pathname = parsed.pathname.substring(idx);
			}
		} else {
			// construct authority
			parsed.authority = parsed.host;
			if (!"".equals(parsed.auth)) {
				parsed.authority = parsed.auth + "@" + parsed.authority;
			}
		}
	}
}
