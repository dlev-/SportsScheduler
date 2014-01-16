package fieldReserveParser;

import java.util.*;

public class FieldReservationParser {

	private static HashMap<String, String> s_fieldNameTranslation = null;

	
    public static void main(String args[])
	{
//		String filePath = "/Users/dlev/Downloads/2010DiscNW-Fall144555-Seattle.pdf";
//		String filePath = "/Users/dlev/Downloads/2010SpringAdult142481-Seattle.pdf";
		String filePath = "/Users/dlev/Downloads/2010DiscNWSummerAdult142491Seattle.pdf";
		ArrayList<FieldReservation> ress = new FieldReservationParser().ParseReservationPdf(filePath);
		for (FieldReservation res : ress)
		{
			System.out.println(res.toString());
		}
	}
	
	public FieldReservationParser()
	{		
	}
	
	public ArrayList<FieldReservation> ParseReservationPdf(String filePath) 
	{
		ArrayList<FieldReservation> toRet = new ArrayList<FieldReservation>();
		
		PDFTextParser parser = new PDFTextParser();
		String parsedText = parser.pdftoText(filePath);
		parsedText = parsedText.substring(parsedText.indexOf("Ending"), parsedText.length());		
		parsedText = parsedText.replace("\b", "");
		
		parsedText = parsedText.replace(",", " ");
		String splitRegex = "[A-Z][a-z]{2} [01][0-9]:[0-6]0 [AP]M[0-3][0-9]-[A-Z][a-z]{2}-[0-9]{2} [01][0-9]:[0-6]0 [AP]M[0-3][0-9]-[A-Z][a-z]{2}-[0-9]{2}";
		int splitRegexLength = "Sun 03:00 PM06-Jun-10 04:00 PM06-Jun-10".length();
		String[] parts = parsedText.split(splitRegex);
		int index = 0;
		for (int ii = 0; ii < parts.length -1; ++ii)
		{
			String fieldNamePart = parts[ii];
			index += fieldNamePart.length();
			fieldNamePart = fieldNamePart.trim();
//			System.out.println(fieldNamePart);
			String dateString = parsedText.substring(index, index + splitRegexLength);
//			System.out.println("DATE = " + dateString);
			index += splitRegexLength;
			
			String translatedFieldName = getFieldNameTranslation(fieldNamePart);
			FieldReservation newRes = parseDateAndCreateReservation(translatedFieldName, dateString);

			for (int jj = 0; jj < toRet.size() && newRes != null; ++jj)
			{
				if (toRet.get(jj).absorbReservation(newRes))
				{
					newRes = null;
				}
			}
			if (newRes != null)
			{
				toRet.add(newRes);
			}
			
		}
		
		return toRet;
	}
	
	private FieldReservation parseDateAndCreateReservation(String fieldName, String dateString)
	{
		String[] dateParts = dateString.split("AM|PM");
		String[] temp = dateParts[0].split(" ");
		String[] timeParts = temp[1].split(":");
		double startTime = Double.parseDouble(timeParts[0]);
		double startFraction = Double.parseDouble(timeParts[1]);
		startTime += startFraction / 60.0;
		if (dateString.indexOf("PM") == dateParts[0].length() && startTime < 12.0)
		{
			startTime += 12.0;
		}
		
		String[] secondDateParts = dateParts[1].split(" ");
		String dateText = secondDateParts[0];
		timeParts = secondDateParts[1].trim().split(":");
		double endTime = Double.parseDouble(timeParts[0]);
		double endFraction = Double.parseDouble(timeParts[1]);
		endTime += endFraction / 60.0;
		if (endTime < startTime)
		{
			endTime += 12.0;
		}
		
		FieldReservation toRet = new FieldReservation(fieldName, dateText, startTime, endTime);
		return toRet;
	}
	
	

	private static String getFieldNameTranslation(String inputFieldName)
	{
		inputFieldName = inputFieldName.trim();
		if (s_fieldNameTranslation == null)
		{
			s_fieldNameTranslation = new HashMap<String, String>();
			s_fieldNameTranslation.put("\"99 ##*  $ 1)", "Bobby Morris Playfield");
			s_fieldNameTranslation.put("0 $=1  $ 1)", "Maplewood Playfield");
			s_fieldNameTranslation.put(">$=1/$  $ 1:? $	*+)", "View Ridge Playfield");
			s_fieldNameTranslation.put("-1 $   $ 1)", "Walt Hundley Playfield");
			s_fieldNameTranslation.put(">**$   $ 1:? $	*+)", "Van Asselt Playfield");
			s_fieldNameTranslation.put("-?#;  $ 1:? $	*+)", "South Park Playfield");
			s_fieldNameTranslation.put("#!8&5", "Magnusun Park 1");
			s_fieldNameTranslation.put("#!8(5", "Magnusun Park 2");			
			s_fieldNameTranslation.put("#!895", "Magnusun Park 3");
			s_fieldNameTranslation.put("#!8'5", "Magnusun Park 4");
			s_fieldNameTranslation.put("#!8?", "Magnusun Park 5");
			s_fieldNameTranslation.put("#!8=", "Magnusun Park 6");
			s_fieldNameTranslation.put("#!8>", "Magnusun Park 7");
			
			s_fieldNameTranslation.put("#!5(", "Magnusun Park 5");
			s_fieldNameTranslation.put("#!5<", "Magnusun Park 6");
			s_fieldNameTranslation.put("#!5@", "Magnusun Park 7");
			s_fieldNameTranslation.put("+$4$ 0:", "Roosevelt High School Playfield");
			s_fieldNameTranslation.put("0$; -$ 2*", "Georgetown Playfield");
			s_fieldNameTranslation.put("$# -$ 2*", "Miller Playfield");
			s_fieldNameTranslation.put(".!!	:  -$ 2*", "Summit Playfield");
			s_fieldNameTranslation.put(".24$; -$ 25: $", "Soundview Playfield");
			s_fieldNameTranslation.put(".:#7 -$ 25: $", "South Park Playfield");
//			s_fieldNameTranslation.put("???", " Playfield");
//			s_fieldNameTranslation.put("???", " Playfield");
		}
		String toRet = inputFieldName;
//		if (s_fieldNameTranslation.containsKey(inputFieldName))
//		{
//			toRet = s_fieldNameTranslation.get(inputFieldName);
//		}

		boolean translated = false;
		for (String key : s_fieldNameTranslation.keySet()) 
		{
			if (!translated && inputFieldName.contains(key))
			{
				toRet = s_fieldNameTranslation.get(key);
			}
		}
		return toRet;
	}
}
