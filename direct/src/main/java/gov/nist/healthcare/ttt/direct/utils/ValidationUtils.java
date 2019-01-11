/**
 This software was developed at the National Institute of Standards and Technology by employees
of the Federal Government in the course of their official duties. Pursuant to title 17 Section 105 of the
United States Code this software is not subject to copyright protection and is in the public domain.
This is an experimental system. NIST assumes no responsibility whatsoever for its use by other parties,
and makes no guarantees, expressed or implied, about its quality, reliability, or any other characteristic.
We would appreciate acknowledgement if the software is used. This software can be redistributed and/or
modified freely provided that any derivative works bear some notice that they are derived from it, and any
modified versions bear some notice that they have been modified.

Project: NWHIN-DIRECT
Authors: Frederic de Vaulx
		Diane Azais
		Julien Perugini
 */

package gov.nist.healthcare.ttt.direct.utils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class ValidationUtils {
	final static String randomString = "[0-9,a-z,_,\\-,.]+"; // case insensitive
	public final static String messageIdStringFormat = "<" + ValidationUtils.randomString + "@" + ValidationUtils.randomString + ">";
	public static Date date;
	public static SimpleDateFormat simpleDateFormat;
	final static SimpleDateFormat dateFormat1 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	final static SimpleDateFormat dateFormat2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm Z");
	final static SimpleDateFormat[] acceptedSdfs = {dateFormat1, dateFormat2};
	public static boolean multipart = false;
	public static final String domainNameFormat = "[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4}";
	
	// MDN variables
	final static String atom = "[0-9,a-z,A-Z]*";
	final static String text = "[0-9,a-z,A-Z,_,.,\\-]*";
	final static String textWithSpace = "[0-9,a-z,A-Z,_,.,\\-,\\s]*";
	final static String actionMode = "(manual-action|automatic-action)";
	final static String sendingMode = "(mdn-sent-manually|mdn-sent-automatically)";
	final static String dispositionType = "(displayed|processed|deleted|dispatched|denied|failed)";
	final static String dispositionModifier = "(error|" + atom + ")";
	
	// Dates patterns
	final static String dayOfWeek = "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)";
	final static String time = "([01]?[0-9]|2[0-3])(:[0-5][0-9]){1,2}";
	final static String timezone = "[-+]((0[0-9]|1[0-3])([03]0|45)|1400)";
	final static String letterTimezone = "\\([A-Z]*\\)";
	final static String whitespace = "\\s";
	final static String unlimitedWhiteSpace = "(" + whitespace + ")*";
	final static String datePattern = "(31 (Jan|Mar|May|Jul|Aug|Oct|Dec)" +    			// 31th of each month
			"|30 (Jan|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)" +      		// 30th of each month except Feb
			"|[0-2]?\\d (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))" +  // days: 01-29th or 1-29th for each month
			" " +
			"((19|20)(\\d{2}))";												// years: 1900 to 2099.

	// Enum for Detail status
	public static enum Status {
		   SUCCESS, ERROR, WARNING, INFO
		 }
	
	
	/**
	 * Computes the length of an email domain
	 * 
	 * @param toValidate The string that contains an email string (in a "to" or "from" field for example)
	 * @return the length of the email domain string
	 */
	public static int emailDomainLength(String toValidate){
		String trimmed = toValidate.trim();	 
		String[] splitEmail = trimmed.split("@");
		String rightSide = splitEmail[1];
		int domainLength = 0;
		if (rightSide.endsWith(">")) {
			domainLength = rightSide.length()-1;
		} else { // if the email address does not end with ">" = is standalone
			domainLength = rightSide.length();
		}
		return domainLength;
	}
	
	// Validates email address format as per RFC 2822
	// includes parenthesis and <>
	public static boolean validateEmailAddressFormatRFC2822(String str){
		final String email = "([0-9a-zA-Z]+([_.-]?[0-9a-zA-Z]+)*@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+";  // source: http://tools.netshiftmedia.com/regexlibrary/
		final String emailWithName = "([0-9,a-z,_,-]+ )*<" + email + ">";
		final String fromFormat =  email + "|" + emailWithName;
		Pattern pattern = Pattern.compile(fromFormat, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(str);
		if(matcher.matches()) {
			return true;
		}
		return false;
	}
	
	// Validates email address format as per RFC 822
	// excludes parenthesis and <>
		public static boolean validateEmailAddressFormatRFC822(String str){
			final String email = "([0-9a-zA-Z]+([_.-]?[0-9a-zA-Z]+)*@[0-9a-zA-Z]+[0-9,a-z,A-Z,.,-]*(.){1}[a-zA-Z]{2,4})+";  // source: http://tools.netshiftmedia.com/regexlibrary/
			Pattern pattern = Pattern.compile(email, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(str);
			if(matcher.matches()) {
				return true;
			}
			return false;
		}

	/**
	 * Finds a multipart message boundary.
	 * @param headerContents
	 * @return
	 */
	public static String findBoundary(String[] headerContents) {
		String boundary = "";
	
		for (int i = 0;i<headerContents.length;i++){
			if (headerContents[i].contains("boundary=")) {
	
				String[] contentType_split = (headerContents[i]).split("boundary=");
				String contentType_split_right = contentType_split[1];
				String[] temp = contentType_split_right.split("\"", -2); // splits again anything that is after the boundary expression, just in case
				temp = contentType_split_right.split(";", -2);
				for (int j = 0; j<temp.length ; j++){
					if (temp[j].contains("--")) {
						boundary = temp[j];
						ValidationUtils.multipart = true;
					}
					else {
//						logger.error("Validation error: the message is multipart but no boundary is defined.");
					}
				}
			}
		}
		return boundary;
	
	}

	// Get the headers and contents of the specified body part of the MimeMessage
	public static ArrayList<String[]> getBodyHeadersAndContent(BodyPart body) {
		Enumeration<?> headers = null;
		try {
			headers = body.getAllHeaders();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	    
	    ArrayList<String[]> res = new ArrayList<String[]>();
	    
	    ArrayList<String> headerName = new ArrayList<String>();
	    ArrayList<String> contentName = new ArrayList<String>();
	    
	    while (headers.hasMoreElements()) {
	    	Header h = (Header) headers.nextElement();
	    	headerName.add(h.getName());
	    	contentName.add(h.getValue());
	    }
	    
	    String[] hName = new String[headerName.size()];
	    String[] cName = new String[contentName.size()];
	    
	    for(int k=0;k<headerName.size();k++) {
	    	hName[k] = headerName.get(k).toLowerCase();
	    	cName[k] = contentName.get(k).toLowerCase();
	    }
	    
	    res.add(hName);
	    res.add(cName);
	    
		return res;
	}

	
	// Get the headers and contents of the specified body part of the MimeMessage
	@SuppressWarnings("unchecked")
	public static InternetHeaders getHeadersAndContent(BodyPart body) {
		Enumeration<Header> headers = null;
		InternetHeaders retVal = new InternetHeaders();
		try {
			headers = body.getAllHeaders();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		

	    while (headers.hasMoreElements()) {
	    	Header h = (Header) headers.nextElement();
	    	System.out.println(h.getName() + " " + h.getValue());
	    	retVal.setHeader(h.getName(), h.getValue());
	    }

		return retVal;
	}

	
	
	// Get the body part
	public static BodyPart getBodyPart(MimeMessage msg, int i) {
		try {
			if (msg.isMimeType("multipart/signed")) {
				
				MimeMultipart test = (MimeMultipart) msg.getContent();
				
				return test.getBodyPart(i);
			} else {
				return null;
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Get the header and content arrays
	public static ArrayList<String[]> getHeadersAndContent(MimeMessage msg) {
	
	    Enumeration<?> headers = null;
		try {
			headers = msg.getAllHeaders();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	    
	    ArrayList<String[]> res = new ArrayList<String[]>();
	    
	    ArrayList<String> headerName = new ArrayList<String>();
	    ArrayList<String> contentName = new ArrayList<String>();
	    
	    while (headers.hasMoreElements()) {
	    	Header h = (Header) headers.nextElement();
	    	headerName.add(h.getName());
	    	contentName.add(h.getValue());
	    }
	    
	    String[] hName = new String[headerName.size()];
	    String[] cName = new String[contentName.size()];
	    
	    for(int k=0;k<headerName.size();k++) {
	    	hName[k] = headerName.get(k).toLowerCase();
	    	cName[k] = contentName.get(k).toLowerCase();
	    }
	    
	    res.add(hName);
	    res.add(cName);
	    
		return res;
	}

	/**
	 * Parses a date string using all available formats. There is no safeguard
	 * for the case where multiple parsers could validate the same date.
	 * @param dateString the string that contains the date to parse.
	 * @return a Date object when parsing is successful, else null. 
	 * @throws ParseException Ignore the unsuccessful parsers.
	 */
	public static Date parseDate(String dateString) throws ParseException {
	
		for (int i = 0 ; i<ValidationUtils.acceptedSdfs.length ;i++){
			SimpleDateFormat sdf = ValidationUtils.acceptedSdfs[i];
			try{
				return sdf.parse(dateString);
			} catch (ParseException e){
				// Ignore and try next parser
			}
		}
		return null;
	}

	// Get the MimeMessage from the byte array
	public static MimeMessage getMimeMessage(byte[] mail) {
		Properties props = System.getProperties();
	
	    Session session = Session.getDefaultInstance(props, null);
	    
	    InputStream is = new ByteArrayInputStream(mail);
	    
	    MimeMessage msg = null;
		try {
			msg = new MimeMessage(session, is);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	    
	    return msg;
	}

	// Get the number of part in the MimeMessage
	public static int getNbBodyPart(MimeMessage msg) {
		
		try {
			if (msg.isMimeType("multipart/signed")) {
				
				MimeMultipart test = (MimeMultipart) msg.getContent();
	
				return test.getCount();
			} else {
				return 0;
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Converts the byte[] array (output of the smtp parser) into a String array for easier handling
	 * and converts all lines of the message to lower case.
	 * 
	 * @param message
	 */
	public static String[] splitMessage(byte[] message){
	
	
		String SMTPMessage = new String(message);
		//er.detail(SMTPMessage);
	
		String[] splitMessage = SMTPMessage.split("\n");
		String str = "";
	
		// Additional verification that no extra line breaks subsist at the end of the lines in the SMTP message.
		for (int i=0; i<splitMessage.length; i++){
			str = splitMessage[i]; 
			if (str.endsWith("\n"))splitMessage[i] = str.substring(0, str.length()-1);
		}
	
		// Removes the first line of the message and converts each line to lower case only.
		// This line does not begin by a "\n" as all lines in an SMTP message should, so it has to be deleted. Ref.: SMTP spec.
		int length = splitMessage.length - 1;
		String[] splitMessageCorrected = new String[length];
		String lowerCase = "";
		for (int i=0; i<length; i++){
			lowerCase = (splitMessage[i+1]).toLowerCase();
			splitMessageCorrected[i] = lowerCase;
		}
	
		// Displays first three lines of the message for error checking
		//er.detail(splitMessageCorrected[0]+splitMessageCorrected[1]+splitMessageCorrected[2]);
	
		return splitMessageCorrected;
	}

	public static void printValidationOk(){
		System.out.println("	Validation status: Ok.");
	}

	public static void setDate(Date dateFromMsg) {
		ValidationUtils.date = dateFromMsg;
	}


	public static Date getDate() {
		return date;
	}

	public static void setSimpleDateFormat(SimpleDateFormat sdf) {
		simpleDateFormat = sdf;
	}

	public static SimpleDateFormat getSimpleDateFormat() {
		return simpleDateFormat;
	}

	public static String getMessageIdStringFormat() {
		return messageIdStringFormat;
	}

	/**
	 * Splits a message into two tables: header names and header contents. Regular text from the body of the message is put in
	 * the "header contents" table.
	 * 
	 * At the same time, tests if the message is simple or multipart to avoid parsing the whole message again.
	 * 
	 * @param message The input message.
	 * @return 
	 */
	public static ArrayList<String[]> splitHeaders(String[] message){
		ArrayList<String[]> splitHeaders = new ArrayList<String[]>();
		String[] headerNames = new String[message.length]; // stores header names from the message
		String[] headerContents = new String[message.length]; // stores header contents from the message
	
		for (int i=0; i<message.length; i++){
			// some headers such as "Received" have too many lines and need to be addressed differently
			if ((message[i]).contains(": ")){ // if the line includes a header name then split it
				String[] splitHeader = (message[i]).split(": "); // gives us a table that contains 2 elements: the current header name and its contents
				headerNames[i] = splitHeader[0];
				headerContents[i] = splitHeader[1];
				//er.detail(headerNames[i]);
			} else {
				headerContents[i] = message[i]; // else if there is no header name then store the whole line in the "Contents" table
				//er.detail(headerContents[i]);
			}									// the matching line in the "Headers Names" table stays empty
	
			// Tests if the message is simple or multipart
			if ((message[i]).contains("multipart")) {multipart = true;}
		}
		splitHeaders.add(headerNames);
		splitHeaders.add(headerContents);
		return splitHeaders;
	}	

	public static boolean isAscii(String str){
		boolean isAscii = true;
		for (int i=0;i<str.length();i++){
			if(str.charAt(i) >= 127)
				isAscii = false;
		}
		return isAscii;
	}
	
	public static boolean controlCharAreOnlyCRLFAndTAB(String str) {
		boolean isOnlyCRLF = true;
		for (int i=0;i<str.length();i++){
			if(str.charAt(i) <= 31 || str.charAt(i) >= 127) {
				if(str.charAt(i)==13) {
					if(i<str.length()) {
						if(str.charAt(i+1)!=10)
							isOnlyCRLF = false;
					}
				}
				else if(str.charAt(i)==10) {
					if(i>0) {
						if(str.charAt(i-1)!=13)
							isOnlyCRLF = false;
					}
				}
			}
		}
		return isOnlyCRLF;
	}
	
	public static boolean isOnlyCRLF(String str) {
		boolean isOnlyCRLF = true;
		for (int i=0;i<str.length();i++){
			if(str.charAt(i)==13) {
				if(i<str.length()) {
					if(str.charAt(i+1)!=10)
						isOnlyCRLF = false;
				}
			}
			else if(str.charAt(i)==10) {
				if(i>0) {
					if(str.charAt(i-1)!=13)
						isOnlyCRLF = false;
				}
			}
		}
		return isOnlyCRLF;
	}
	
	public static boolean areLignesInf76Char(String str) {
		boolean bool = true;
		int count = 0;
		for (int i=0;i<str.length();i++){
			count++;
			if(i>0) {
				if(str.charAt(i)==10 && str.charAt(i-1)==13)
					count = 0;	
			}
			
			if(i<str.length()) {
				if(str.charAt(i)==13 && str.charAt(i+1)==10)
					count = 0;
			}
			
			if(count>76) {
				bool = false;
			}
		}
		return bool;
	}
	
	public static String getDatePatternWithSpace() {
		// Pattern with more than one space
		final String datePatternMoreSpace = "(" + dayOfWeek + "," + unlimitedWhiteSpace + ")?" + datePattern + unlimitedWhiteSpace + time + unlimitedWhiteSpace + timezone + "(" +  unlimitedWhiteSpace + letterTimezone + ")" + "?";
		return datePatternMoreSpace;
	}
	
	public static String getDatePattern() {		
		final String pattern = "(" + dayOfWeek + "," + whitespace + ")?" + datePattern + whitespace + time + whitespace + timezone + "(" +  whitespace + letterTimezone + ")" + "?";
		return pattern;
	}
		

	
	public static boolean validateDate(String origDate) {
		Pattern pattern = Pattern.compile(getDatePattern(), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(origDate);
		
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean validateDateWithMoreSpace(String origDate) {
		Pattern pattern = Pattern.compile(getDatePatternWithSpace(), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(origDate);
		
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String getAddrSpecPattern() {
		return "<[0-9,a-z,_,\\-,.,\\!,\\#,\\$,\\%,\\&,\\',\\*,\\+,\\/,\\=,\\?,\\^,\\`,\\{,\\},\\|,\\~]+@[0-9,a-z,_,\\-,.,\\!,\\#,\\$,\\%,\\&,\\',\\*,\\+,\\/,\\=,\\?,\\^,\\`,\\{,\\},\\|,\\~]+>" + "(\\s)*";
	}
	
	public static boolean validateAddrSpec(String addrSpec) {
		Pattern pattern = Pattern.compile(getAddrSpecPattern(), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(addrSpec);
		
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	// MDN methods
	
	public static boolean validateAtomTextField(String field) {
		final String stringPattern =  atom + ";" + text;
		Pattern pattern = Pattern.compile(stringPattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(field);
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean validateDisposition(String disposition) {
		String dispositionPattern = "(" + actionMode + "/" + sendingMode + ")" + ";" +"(\\s)?" + dispositionType;
		dispositionPattern += "(/" + dispositionModifier + "(,\\s" + dispositionModifier + ")*)?";
		Pattern pattern = Pattern.compile(dispositionPattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(disposition);
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String getAtom() {
		return atom;
	}

	public static boolean validateTextField(String textField) {
		Pattern pattern = Pattern.compile(textWithSpace, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(textField);
		if(matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
	
	// End
	
	public static boolean validateEmail(String email) {
		boolean valid = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException e) {
			valid = false;
		}
		return valid;
	}

	public static String getReceivedPart(String receivedField, String clause) {
		String res = "";
		if(receivedField.contains(clause)) {
			receivedField = receivedField.split(clause, 2)[1];
			
			String[] clauses = {" by ", " via ", " with ", " id ", " for ", ";"};
			for(String token : clauses) {
				if(receivedField.contains(token)) {
					res = receivedField.split(token)[0].replaceAll("\\s", "");
					return res;
				}
			}
		} else {
			clause = clause.replaceFirst("\\s", "");
			if(receivedField.startsWith(clause)) {
				return getReceivedPart(receivedField, clause);
			}
		}
		return res;
	}
	
	public static boolean validateReceivedPart(String part, String patternTxt, boolean required) {
		if(!part.equals("")) {
			Pattern pattern = Pattern.compile(patternTxt, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(part);
			if(matcher.matches()) {
				return true;
			} else {
				return false;
			}
		} else {
			return required;
		}
	}
	
	public static String getSingleHeader(Part p, String name) throws MessagingException {
		if(p.getHeader(name) != null) {
			return p.getHeader(name)[0];
		}
		return "";
	}
	
	public static String[] getMultipleHeader(Part p, String name) throws MessagingException {
		if(p.getHeader(name) != null) {
			return p.getHeader(name);
		}
		return new String[0];
	}
	
	public static ArrayList<String> fillArrayLog(Object[] array) {
		ArrayList<String> res = new ArrayList<String>();
		if(array != null) {
			for(int i=0;i<array.length;i++) {
				String trimedFrom = array[i].toString();
				if(array[i].toString().contains("<")) {
					trimedFrom = trimedFrom.split("<")[1];
					trimedFrom = trimedFrom.split(">")[0];
				}
				res.add(trimedFrom);
			}
		}
		return res;
	}
	
	public static Properties getProp() throws Exception{
		Properties prop = new Properties();
		String path = "./application.properties";
		FileInputStream file = new FileInputStream(path);
		prop.load(file);
		file.close();
		return prop;
		}

}
