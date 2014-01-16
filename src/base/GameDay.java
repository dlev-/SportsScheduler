package base;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class GameDay {
	private final static String[] s_dayNames = new DateFormatSymbols().getWeekdays();
	private final String m_dateString;
	private final String m_weekId;
	private final String m_dayOfWeek;
	private final Calendar m_date = Calendar.getInstance();
	private final ArrayList<GameTime> m_gameTimes = new ArrayList<GameTime>();
	
	
	public GameDay(String dateText, String weekId) throws ParseException
	{
		m_dateString = dateText;
		m_weekId = weekId;

		String[] dateParts = dateText.split("/");
		m_date.set(Calendar.YEAR, App.CURRENT_YEAR);
		m_date.set(Calendar.MONTH, Integer.parseInt(dateParts[0]) - 1);
		m_date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[1]));
//		System.out.println(m_date.getTime().toString());
		String dayOfWeek = s_dayNames[m_date.get(Calendar.DAY_OF_WEEK)];
		m_dayOfWeek = dayOfWeek.toLowerCase();
	}
	
	private void AddGameTime(GameTime gt)
	{
		m_gameTimes.add(gt);
	}
	
	
	public String getDateString()
	{
		return m_dateString;
	}

	public static void grabFromXml(String xmlFilePath, ArrayList<GameDay> gamesToFill, ArrayList<String> fieldNamesToFill) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, ParseException 
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//		dbFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		Document doc = builder.parse(xmlFilePath);
		
		XPathFactory xpFactory = XPathFactory.newInstance();
		XPath xpath = xpFactory.newXPath();

		{
			// get the field names (column headers)
			String fieldNameQuery = "//table[@class='gamesched']/thead/tr/td/text()";
			Object result = xpath.evaluate(fieldNameQuery, doc, XPathConstants.NODESET);
			NodeList fieldNameNodes = (NodeList) result;
	
			for (int ii = 1; ii < fieldNameNodes.getLength(); ++ii)
			{
				fieldNamesToFill.add(fieldNameNodes.item(ii).getTextContent());
			}
		}

		// get the rows of the table (dates/times/and availability
		XPathExpression xpathSelectGameDateTime = xpath.compile("td[1]/text()");
		XPathExpression xpathSelectGameAvailability = xpath.compile("td/@style");
		XPathExpression xpathSelectGameIds = xpath.compile("td//tr/td/input[@name]");
		
		String gameTimeRowQuery = "//table[@class='gamesched']/tbody/tr";
		Object result = xpath.evaluate(gameTimeRowQuery, doc, XPathConstants.NODESET);
		NodeList gameTimeRowNodes = (NodeList) result;
		for (int ii = 0; ii < gameTimeRowNodes.getLength(); ++ii)
		{
			Node curNode = gameTimeRowNodes.item(ii);
			String discNWWeekName = curNode.getAttributes().getNamedItem("class").getNodeValue();
			String gameDateTime = (String) xpathSelectGameDateTime.evaluate(curNode, XPathConstants.STRING);
			String[] dateTimeParts = gameDateTime.trim().split(" ");
			String gameDate = dateTimeParts[0];
			String gameTimeText = dateTimeParts[1];
			
			GameTime curGameTime = new GameTime(gameTimeText);
			
			String[] fieldAvailabilityColors = getTextForExpression(curNode, xpathSelectGameAvailability);
			int inputNodeNum = 0;
			NodeList formInputNodes = (NodeList) xpathSelectGameIds.evaluate(curNode, XPathConstants.NODESET);
			if (fieldAvailabilityColors.length != fieldNamesToFill.size()) throw new Error("wrong number of fields somehow");
			for (int fieldNum = 0; fieldNum < fieldAvailabilityColors.length; ++fieldNum)
			{
				String color = fieldAvailabilityColors[fieldNum];
				color = color.substring(color.indexOf(":#") + 2).replace(";", "");
				if (!color.contains("color: rgb(191, 178, 129)") && !color.contains("bfb281"))
				{
					NamedNodeMap curInputNodeAttributes = formInputNodes.item(inputNodeNum).getAttributes();
					String webGameLabel = curInputNodeAttributes.getNamedItem("name").getNodeValue();
					String webGameLabelVal = curInputNodeAttributes.getNamedItem("value").getNodeValue();
					int webGameId = Integer.parseInt(webGameLabel.substring(webGameLabel.indexOf("_") + 1));
					int webGameVal = Integer.parseInt(webGameLabelVal);
					if (webGameVal == 0)
					{
						webGameLabelVal = formInputNodes.item(inputNodeNum + 1).getAttributes().getNamedItem("value").getNodeValue();
						webGameVal = Integer.parseInt(webGameLabelVal);
					}
					inputNodeNum += 2;

					Game newGame = new Game(fieldNum, webGameId, webGameVal);
					curGameTime.addGame(newGame);
				}
			}
			
			if (curGameTime.getGames().size() > 0)
			{
				GameDay curDay = getGameDayForDate(gamesToFill, gameDate, discNWWeekName);
				curDay.AddGameTime(curGameTime);
			}
		}
	}
	
	private static GameDay getGameDayForDate(ArrayList<GameDay> gameDays, String gameDate, String weekId) throws ParseException
	{
		if (gameDays.size() == 0 || !gameDays.get(gameDays.size() - 1).getDateString().equals(gameDate))
		{
			gameDays.add(new GameDay(gameDate, weekId));
		}
		return gameDays.get(gameDays.size() - 1);
	}

	public static void serializeSchedule(ArrayList<GameDay> gameDays, ArrayList<String> fieldNames, String filePath) throws IOException 
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
		int numGameSlots = 0;
		// write header with field names
		for (int ii = 0; ii < c_serializeLeftColumnBuffer - 1; ++ii)
		{
			bw.write(",");
		}
		for (String fieldName : fieldNames)
		{
			bw.write("," + fieldName);
		}
		
		for (GameDay gameDay : gameDays)
		{
			for (GameTime gt : gameDay.getGameTimes())
			{
				bw.newLine();
				bw.write(gameDay.getWeekId());
				bw.write("," + gameDay.getDateString());
				bw.write("," + gt.getTimeString());

				for (int ii = 0; ii < fieldNames.size(); ++ii)
				{
					bw.write(",");					
					for (Game game : gt.getGames())
					{
						if (game.getFieldNum() == ii)
						{
							bw.write(game.getWebGameId() + "|" + game.getWebGameValue() + " ");
							bw.write(game.getSerializeString());
							++numGameSlots;
						}
					}
				}
			} 
		}
		
		bw.close();
		System.out.println("Total number of game slots: " + numGameSlots);
	}

	public ArrayList<GameTime> getGameTimes() {
		return m_gameTimes;
	}
	public String getWeekId()
	{
		return m_weekId;
	}
	public String getDayOfWeek()
	{
		return m_dayOfWeek;
	}

	public static void deserializeSchedule(String filePath, ArrayList<GameDay> gameDays, ArrayList<String> fieldNames, MatchupSet matchups) throws IOException, ParseException 
	{
		BufferedReader br = new BufferedReader(new FileReader(filePath));

		// handle header
		String line = br.readLine();
		String[] lineParts = line.split(",");
		for (int ii = c_serializeLeftColumnBuffer; ii < lineParts.length; ++ii)
		{
			fieldNames.add(lineParts[ii].trim());
		}
		
		while((line = br.readLine()) != null)
		{
			lineParts = line.split(",");
			String weekId = lineParts[0];
			String gameDate = lineParts[1];
			String time = lineParts[2];
			GameDay curDay = getGameDayForDate(gameDays, gameDate, weekId);
			GameTime curTime = new GameTime(time);
			
			for (int ii = c_serializeLeftColumnBuffer; ii < lineParts.length; ++ii)
			{
				if (lineParts[ii] != null && lineParts[ii].length() > 0)
				{
					int fieldNum = ii - c_serializeLeftColumnBuffer;
					String[] gameEntryParts = lineParts[ii].split(" ");
					String[] webIdParts = gameEntryParts[0].split("\\|");
					
					int webIdNum = Integer.parseInt(webIdParts[0]);
					int webIdVal = Integer.parseInt(webIdParts[1]);
					Game gameSlot = new Game(fieldNum, webIdNum, webIdVal);
					
					if (gameEntryParts.length == 3)
					{
						assert(matchups != null);
						int homeTeamId = Integer.parseInt(gameEntryParts[1].substring(0,gameEntryParts[1].indexOf(':')));
						int awayTeamId = Integer.parseInt(gameEntryParts[2].substring(0,gameEntryParts[2].indexOf(':')));
						Team homeTeam = matchups.getTeamById(homeTeamId);
						Team awayTeam = matchups.getTeamById(awayTeamId);
						Matchup m = new Matchup(homeTeam, awayTeam);
						gameSlot.setMatchup(m);
					}
					curTime.addGame(gameSlot);
				}
			}
			
			curDay.AddGameTime(curTime);		
		}
		
		br.close();
	}

	
	private static String[] getTextForExpression(Node root, XPathExpression expr) throws XPathExpressionException
	{
		Object result = expr.evaluate(root, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;

		ArrayList<String> foundStrings = new ArrayList<String>();
		for (int ii = 0; ii < nodes.getLength(); ++ii)
		{
			String foundText = nodes.item(ii).getNodeValue();
			if (!foundText.trim().equals(""))
			{
				foundStrings.add(foundText);
			}
		}

		String[] toRet = foundStrings.toArray(new String[] {});
		return toRet;
	}
	
	private static final int c_serializeLeftColumnBuffer = 3;


	public Calendar getDate() {
		return m_date;
	}
}
