package base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.Pair;

import constraints.ByeConstraint;
import constraints.DayOfWeekConstraint;
import constraints.DoubleHeaderConstraint;
import constraints.FieldPreferenceConstraint;
import constraints.NonConsecutiveGameConstraint;
import constraints.OneGamePerWeekConstraint;
import constraints.ScheduleConstraint;
import constraints.TimeBetweenRematchConstraint;
import constraints.TimeOfDayConstraint;


public class Team {	
	private final String m_name;
	private final int m_id;
	private final int m_division;
	private final ArrayList<GameSlotInfo> m_gameSlots = new ArrayList<GameSlotInfo>();
	private final CrossoverPreference m_crossoverPreference;
	private double m_energyCost;
	private GameSlotInfo m_worstSlot;
	private final ArrayList<ScheduleConstraint> m_constraints = new ArrayList<ScheduleConstraint>();
	private final static GsiComparator s_gsiComparator = new GsiComparator();
	private final String m_byeDates;
	
	public Team(String name, int id, int division, CrossoverPreference cp, String byeDates)
	{
		m_name = sanitizeTeamName(name);
		m_id = id;
		m_division = division;
		m_crossoverPreference = cp;
		m_constraints.add(new NonConsecutiveGameConstraint());
		m_constraints.add(new OneGamePerWeekConstraint());
		m_byeDates = byeDates;
		invalidateCachedEnergy();
	}
	

	public String getName() {
		return m_name;
	}

	public int getId() {
		return m_id;
	}

	public int getDivision() {
		return m_division;
	}

	public String getByeDates() {
		return m_byeDates;
	}
	
	public CrossoverPreference getCrossoverPreference() {
		return m_crossoverPreference;
	}

	public boolean Equals(Team oTeam)
	{
		return getId() == oTeam.getId();
	}
	
	public String toString()
	{
		return getName();
	}
	
	public static int howBadIsCrossoverGame(Team homeTeam, Team awayTeam) 
	{
		int toRet = 0;
		if (homeTeam.equals(awayTeam))
		{
			toRet = 1000;
		}
		else
		{
			// make sure the home team is a higher division
			if (homeTeam.getDivision() < awayTeam.getDivision())
			{
				Team temp = awayTeam;
				awayTeam = homeTeam;
				homeTeam = temp;
			}
			
			int homeDiv = homeTeam.getDivision();
			int awayDiv = awayTeam.getDivision();
			
			int divDiff = homeDiv - awayDiv;
			if (divDiff > 1)
			{
				toRet = (int)Math.pow(2, divDiff);
			}
			else if (! ((homeTeam.getCrossoverPreference() == CrossoverPreference.UP || 
					     homeTeam.getCrossoverPreference() == CrossoverPreference.UP_OR_DOWN) &&
						(awayTeam.getCrossoverPreference() == CrossoverPreference.DOWN || 
						 awayTeam.getCrossoverPreference() == CrossoverPreference.UP_OR_DOWN)))
			{
				toRet = 1;
			}
		}
		return toRet;
	}
	
	// The serialization and deserialization are reading/writing to a csv file.
	public static void deserializeTeams(String filePath, ArrayList<Team> toFill, ArrayList<GameDay> gameDays, ArrayList<String> fieldNames) throws IOException
	{
		ArrayList<String> gameDates = new ArrayList<String>();
		ArrayList<String> daysOfWeek = new ArrayList<String>();
		int earliestGameHour = 24;
		int latestGameHour = 0;
		if (gameDays != null)
		{
			for (GameDay gd : gameDays)
			{
				gameDates.add(gd.getDateString());
				if (!daysOfWeek.contains(gd.getDayOfWeek()))
				{
					daysOfWeek.add(gd.getDayOfWeek());
				}
				for (GameTime gt : gd.getGameTimes())
				{
					int gameHour = gt.getHour();
					earliestGameHour = Math.min(earliestGameHour, gameHour);
					latestGameHour = Math.max(latestGameHour, gameHour);
				}
			}
		}
		
		
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = br.readLine();
		// can ignore the first line.
		
		while ((line = br.readLine()) != null)
		{
			String[] lineParts = line.split(",");
			String teamName = lineParts[0];
			int teamId = Integer.parseInt(lineParts[1]);
			int division = Integer.parseInt(lineParts[2]);
			String crossoverPrefText = lineParts[3];
			CrossoverPreference crossoverPref = CrossoverPreference.valueOf(crossoverPrefText);
			Team newTeam = new Team(teamName, teamId, division, crossoverPref, lineParts[4]);
			if (gameDays != null && gameDays.size() > 0)
			{
				if (hasNonEmptyValue(lineParts, 4))
				{
					String[] byeDays = lineParts[4].split(" ");
					for (String byeDay : byeDays)
					{
						if (!gameDates.contains(byeDay))
						{
							throw new Error("The bye date '" + byeDay + "' for team: " + teamName + " was not valid");
						}
					}
					newTeam.m_constraints.add(new ByeConstraint(byeDays));
				}
				if (hasNonEmptyValue(lineParts, 5))
				{
					String[] badDaysOfWeek = lineParts[5].split(" ");
					for (int ii = 0; ii < badDaysOfWeek.length; ++ii)
					{
						badDaysOfWeek[ii] = badDaysOfWeek[ii].toLowerCase();
					}
					for (String badDay : badDaysOfWeek)
					{
						if (!daysOfWeek.contains(badDay))
						{
							throw new Error("The day of the week '" + badDay + "' for team: " + teamName + " was not a day when games are played");
						}
					}
					newTeam.m_constraints.add(new DayOfWeekConstraint(badDaysOfWeek));
				}
				int minStartHour = -1;
				int maxStartHour = -1;
				if (hasNonEmptyValue(lineParts, 6))
				{
					minStartHour = Integer.parseInt(lineParts[6]);
					if (minStartHour < earliestGameHour || minStartHour > latestGameHour)
					{
						throw new Error("The minimum start hour: " + minStartHour + " for team " + teamName + " was before the earliest game (" + earliestGameHour + ") or after the latest game(" + latestGameHour + ").");
					}
				}
				if (hasNonEmptyValue(lineParts, 7))
				{
					maxStartHour = Integer.parseInt(lineParts[7]);
					if (maxStartHour < earliestGameHour || maxStartHour > latestGameHour)
					{
						throw new Error("The maximum start hour: " + maxStartHour + " for team " + teamName + " was before the earliest game (" + earliestGameHour + ") or after the latest game(" + latestGameHour + ").");
					}
				}
				if (minStartHour >= 0 || maxStartHour >= 0)
				{
					newTeam.m_constraints.add(new TimeOfDayConstraint(minStartHour, maxStartHour));
				}
				
				int minGamesInRow = 1;
				int maxGamesInRow = 2;
				if (hasNonEmptyValue(lineParts, 8))
				{
					minGamesInRow = Integer.parseInt(lineParts[8]);
				}
				if (hasNonEmptyValue(lineParts, 9))
				{
					maxGamesInRow = Integer.parseInt(lineParts[9]);
				}
				newTeam.m_constraints.add(new DoubleHeaderConstraint(minGamesInRow, maxGamesInRow));

				if (hasNonEmptyValue(lineParts, 10))
				{
					ArrayList<Integer> goodFieldIds = new ArrayList<Integer>();
					String[] goodFieldNames = lineParts[10].split(";");
					for (String goodFieldName : goodFieldNames)
					{
						int numGoodFieldIds = goodFieldIds.size();
						goodFieldName = goodFieldName.trim().toLowerCase();
						for (int fieldNum = 0; fieldNum < fieldNames.size(); ++fieldNum)
						{
							if (fieldNames.get(fieldNum).toLowerCase().contains(goodFieldName))
							{
								goodFieldIds.add(fieldNum);
							}
						}
						assert (numGoodFieldIds != goodFieldIds.size());
					}
					newTeam.m_constraints.add(new FieldPreferenceConstraint(goodFieldIds));
				}
			}
			toFill.add(newTeam);
		}
		br.close();
	}
	
	private static boolean hasNonEmptyValue(String[] lineParts, int index)
	{
		return lineParts.length > index && 
			   lineParts[index] != null &&
			   lineParts[index].length() > 0;
	}
	
	public static void serializeTeams(ArrayList<Team> teams, String filePath) throws IOException
	{
		String columnHeader = "name,id,pool,crossover preference,bye days,bad day of week,earliest start hour (24hr),latest start hour (24hr),min games in a row,max games in a row,field pref";
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
		bw.write(columnHeader);
		bw.newLine();
		for (Team team : teams)
		{
			bw.write(team.getName() + "," + 
					 team.getId() + "," + 
					 team.getDivision() + "," + 
					 team.getCrossoverPreference() + "," +
					 "," +	// bye days
					 "," +	// bad day of week
					 "," +  // earliest start hour
					 "," +  // latest start hour
					 "," +  // min games in a row
					 "2"    // max games in a row
					 );
			bw.newLine();
		}
		bw.close();
	}



	public static void grabFromXml(String xmlFilePath, boolean applyDefaultCrossovers, ArrayList<Team> toFill) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException 
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//		dbFactory.setNamespaceAware(true); // never forget this!
		DocumentBuilder builder = dbFactory.newDocumentBuilder();
		Document doc = builder.parse(xmlFilePath);
		
		XPathFactory xpFactory = XPathFactory.newInstance();
		XPath xpath = xpFactory.newXPath();
		// <caption title="A Pool">A</caption>
//		XPathExpression xpathSelectDivisionName = xpath.compile("caption/text()");
		XPathExpression xpathSelectTeam = xpath.compile("tbody/tr/td/a");
		
		String selectDivisionsQuery = "//table[@class='table table-hover table-bordered btp-standings-table']";
		Object result = xpath.evaluate(selectDivisionsQuery, doc, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;
		
		for (int divisionNum = 0; divisionNum < nodes.getLength(); ++divisionNum)
		{
			Node divisionNode = nodes.item(divisionNum);
//			String divisionName = (String) xpathSelectDivisionName.evaluate(divisionNode, XPathConstants.STRING);
			NodeList teamNodes = (NodeList) xpathSelectTeam.evaluate(divisionNode, XPathConstants.NODESET);
			for (int ii = 0; ii < teamNodes.getLength(); ++ii)
			{
				Node curNode = teamNodes.item(ii);
				String teamName = curNode.getTextContent();
				String teamUrl = curNode.getAttributes().getNamedItem("href").getNodeValue();
				teamUrl = teamUrl.substring(teamUrl.indexOf("/teams/") + 7);
				if (teamUrl.endsWith("/"))
				{
					teamUrl = teamUrl.substring(0, teamUrl.length() - 1);
				}
				int teamId = Integer.parseInt(teamUrl);

				CrossoverPreference crossPref = CrossoverPreference.NO_CROSSOVERS;
				if (applyDefaultCrossovers)
				{
					if (divisionNum == 0)
					{
						crossPref = CrossoverPreference.DOWN;
					} 
					else if (divisionNum == nodes.getLength() - 1)
					{
						crossPref = CrossoverPreference.UP;
					}
					else
					{
						crossPref = CrossoverPreference.UP_OR_DOWN;
					}
				}
				Team newTeam = new Team(teamName, teamId, divisionNum, crossPref, "");
				toFill.add(newTeam);
			}

		}

	}
	

	private static String sanitizeTeamName(String rawTeamName)
	{
		String toRet = rawTeamName.replaceAll(",", "");
		toRet = toRet.replaceAll("\\s", " ");
		return toRet;
	}


	
	public String getShortIdName() 
	{
		String shortTeamName = getName();
		int shortNameLength = 10;
		if (shortTeamName.length() > shortNameLength)
		{
			shortTeamName = shortTeamName.substring(0, shortNameLength);
		}
		shortTeamName = shortTeamName.replace(' ', '_');
		return getId() + ":" + shortTeamName;
	}

	public double getCost()
	{
		if (m_energyCost < 0)
		{
			m_worstSlot = calcCost();
		}
		return m_energyCost;
	}
	
	public void prepareForOpt(ArrayList<String> fieldNames)
	{		
		boolean containsRematchConstraint = false;
		for (ScheduleConstraint sc : m_constraints)
		{
			if (sc instanceof TimeBetweenRematchConstraint)
			{
				containsRematchConstraint = true;
			}
		}
		if (!containsRematchConstraint)
		{
			HashSet<Team> rematchOpponents = new HashSet<Team>();
			HashSet<Team> opponents = new HashSet<Team>();
			for (GameSlotInfo gsi : m_gameSlots)
			{
				for (Team t : gsi.getGame().getMatchup().getTeams())
				{
					if (! this.equals(t) &&
						! opponents.add(t))
					{
						rematchOpponents.add(t);
					}
				}
			}
			if (rematchOpponents.size() > 0)
			{
				m_constraints.add(new TimeBetweenRematchConstraint(rematchOpponents));
			}
		}
		Collections.sort(m_constraints);
		
		for (ScheduleConstraint constraint : m_constraints)
		{
			if (constraint instanceof DoubleHeaderConstraint)
			{
				((DoubleHeaderConstraint)constraint).setFieldNames(fieldNames);
			}
		}
	}
	
	private GameSlotInfo calcCost() 
	{
		double costScore = 0;
		GameSlotInfo toRet = null;
		
		Collections.sort(m_gameSlots, s_gsiComparator);
		
		for (ScheduleConstraint constraint : m_constraints)
		{
			Pair<GameSlotInfo, Double> slotCostPair = constraint.eval(m_gameSlots);
			if (slotCostPair.getFirst() != null)
			{
				costScore += slotCostPair.getSecond();
				if (toRet == null)
				{
					toRet = slotCostPair.getFirst();
				}
			}
		}
		
		m_energyCost = costScore;
		return toRet;
	}


	public GameSlotInfo getWorstGsi()
	{
		return m_worstSlot;
	}
	
	private void invalidateCachedEnergy()
	{
		m_energyCost = -1;
		m_worstSlot = null;
	}

	public void replaceGameSlot(GameSlotInfo oldGsi, GameSlotInfo newGsi)
	{
		assert(oldGsi != null);
		assert(newGsi != null);
		assert(oldGsi.getGame().getMatchup().isTeamInMatchup(this));
		if (newGsi.getGame().getMatchup() != null &&
			newGsi.getGame().getMatchup().isTeamInMatchup(this))
		{
			// do nothing...
		}
		else
		{
			int index = m_gameSlots.indexOf(oldGsi);
			assert (index >= 0);
			assert (!m_gameSlots.contains(newGsi));
			m_gameSlots.set(index, newGsi);
			invalidateCachedEnergy();
		}
	}
	
	public void clearGameSlots() {
		m_gameSlots.clear();
		invalidateCachedEnergy();
	}

	public void addGameSlot(GameSlotInfo gsi) {
		m_gameSlots.add(gsi);
	}
	
	public static class GsiComparator implements Comparator<GameSlotInfo>
	{
		public int compare(GameSlotInfo o1, GameSlotInfo o2) {
			int toRet = o1.getGameDay().getDateString().compareTo(o2.getGameDay().getDateString());
			if (toRet == 0)
			{
				toRet = o1.getGameTime().getHour() < o2.getGameTime().getHour() ? -1 : 1;
			}
			return toRet;
		}
		
	}

	public void verifyTeamHasGsi(GameSlotInfo gsi) {
		assert(m_gameSlots.contains(gsi));
		assert(gsi.getGame().getMatchup().isTeamInMatchup(this));
	}
}