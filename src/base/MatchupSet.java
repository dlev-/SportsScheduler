package base;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class MatchupSet {

	public MatchupSet(ArrayList<Team> teams, int inPoolGames, int crossoverGames)
	{
		InitTeamMap(teams);
		m_matchups.addAll(generateInPoolMatchups(inPoolGames));
		m_matchups.addAll(generateCrossoverMatchups(crossoverGames));
	}
	
	private MatchupSet(ArrayList<Team> teams)
	{
		InitTeamMap(teams);
	}
	

	public void serializeMatchups(String fileName) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (Team team : m_teams)
		{
			bw.write("," + team.getShortIdName());
		}
		bw.write(",total");
		bw.newLine();

		ArrayList<Matchup> remainingMatchups = new ArrayList<Matchup>(m_matchups);
		
		for (int ii = 0; ii < m_teams.size(); ++ii)
		{
			Team curTeam = m_teams.get(ii);
			ArrayList<Matchup> matchupsToWrite = new ArrayList<Matchup>();
			for (int jj = remainingMatchups.size() - 1; jj >= 0; --jj)
			{
				if (remainingMatchups.get(jj).isTeamInMatchup(curTeam))
				{
					Matchup temp = remainingMatchups.get(remainingMatchups.size() - 1);
					matchupsToWrite.add(remainingMatchups.get(jj));
					remainingMatchups.set(jj, temp);
					remainingMatchups.remove(remainingMatchups.size() - 1);
				}
			}
			bw.write(curTeam.getShortIdName());
			for (Team opponent : m_teams)
			{
				int numMatchups = 0;
				if (!opponent.equals(curTeam))
				{
					for (Matchup m : matchupsToWrite)
					{
						if (m.isTeamInMatchup(opponent))
						{
							++numMatchups;
						}
					}
				}
				bw.write(",");
				if (opponent.equals(curTeam))
				{
					bw.write("0");
				} 
				else if (numMatchups != 0)
				{
					bw.write(Integer.toString(numMatchups));	
				}
			}
			int numTeams = m_teams.size();
			String excelColString = getExcelColString(ii + 1);
			int excelRowNum = ii + 2;
			bw.write(",=SUM(" + excelColString + "2:" + excelColString + (numTeams + 1) + ") + " + 
					   "SUM(B" + excelRowNum + ":" + getExcelColString(numTeams) + excelRowNum + ")");
			bw.write("," + curTeam.getName());
			bw.newLine();
		}
		bw.close();
	}
	
	// zero indexed column number
	private static String getExcelColString(int colNumber) 
	{
		String converted = "";
        // Repeatedly divide the number by 26 and convert the
        // remainder into the appropriate letter.
        while (colNumber >= 0)
        {
            int remainder = colNumber % 26;
            converted = (char)(remainder + 'A') + converted;
            colNumber = (colNumber / 26) - 1;
        }

        return converted;
	}

	public Team getTeamById(int teamId)
	{
		return m_idToTeamLookup.get(teamId);
	}
	
	public static MatchupSet deserializeMatchups(ArrayList<Team> teams, String fileName) throws IOException
	{
		MatchupSet toRet = new MatchupSet(teams);
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = br.readLine();
		String[] lineParts = line.split(",");
		if (!lineParts[lineParts.length - 1].equalsIgnoreCase("total")) throw new Error("Badly formatted line");
		int numTeams = lineParts.length - 2;
		int[] teamIds = new int[numTeams];
		for (int ii = 0; ii < numTeams; ++ii)
		{
			teamIds[ii] = getTeamId(lineParts[ii + 1]);
		}
		
		while ((line = br.readLine()) != null)
		{
			lineParts = line.split(",");
			int teamId = getTeamId(lineParts[0]);
			
			for (int ii = 0; ii < teamIds.length; ++ii)
			{
				String curVal = lineParts[ii+1];
				if (curVal != null && curVal.length() > 0)
				{
					int numMatches = Integer.parseInt(curVal);
					for (int jj = 0; jj < numMatches; ++jj)
					{
						Matchup newMatchup = new Matchup(toRet.m_idToTeamLookup.get(teamId), 
								                         toRet.m_idToTeamLookup.get(teamIds[ii]));
						toRet.m_matchups.add(newMatchup);
					}
				}
			}			
		}
		br.close();
		return toRet;
	}
	private static int getTeamId(String serializedTeamId)
	{
		return Integer.parseInt(serializedTeamId.substring(0, serializedTeamId.indexOf(":")));
	}

		
	private void InitTeamMap(ArrayList<Team> teams) 
	{
		m_teams = teams;
		for (Team team : teams)
		{
			m_idToTeamLookup.put(team.getId(), team);
		}		
	}


	private ArrayList<Matchup> generateCrossoverMatchups(int crossoverGames) 
	{
		ArrayList<Team> teams = new ArrayList<Team>(m_teams);
		Collections.shuffle(teams);
		// create an initial set of crossover games
		ArrayList<Matchup> toRet = createMatchupsForTeamGroup(teams, crossoverGames);

		int maxSaIters = 10000;
		Random rand = Scheduler.getRandom();
		int numBadMatchups = countInvalidCrossovers(toRet);
		// optimize crossover set
		for (int iteration = 0; iteration < maxSaIters && numBadMatchups > 0; ++iteration)
		{
			double randStepCutoff = (double)(maxSaIters - iteration) / (double)(maxSaIters);
			MatchOptMove matchChange = null;
			boolean performRandMove = rand.nextDouble() < randStepCutoff;
			if (performRandMove)
			{
				//perform random step
				int m1 = rand.nextInt(toRet.size());
				int m2;
				do
				{
					m2 = rand.nextInt(toRet.size());
				}
				while (m2 == m1);
				
				matchChange = new MatchOptMove(m1, m2, rand.nextBoolean());
			} 
			else
			{
				// perform hillclimbing step
				int badMatchup = -1;
				for (int ii = 0; ii < toRet.size() && badMatchup < 0; ++ii)
				{
					if (0 < Team.howBadIsCrossoverGame(toRet.get(ii).getHomeTeam(), toRet.get(ii).getAwayTeam()))
					{
						badMatchup = ii;
					}
				}
				for (int ii = 0; ii < toRet.size(); ++ii)
				{
					MatchOptMove potentialMove = new MatchOptMove(badMatchup, ii, rand.nextBoolean());
					potentialMove.applyMove(toRet);
					if (countInvalidCrossovers(toRet) < numBadMatchups)
					{
						matchChange = potentialMove;
						ii = toRet.size();
					}
					potentialMove.revertMove(toRet);
				}
			}
			
			if (matchChange != null)
			{
				matchChange.applyMove(toRet);
				numBadMatchups = countInvalidCrossovers(toRet);
			}
			
			if (iteration %20 == 0)
			{
				System.out.println(iteration + ": " + numBadMatchups);
				Collections.shuffle(toRet);
			}
		}
		System.out.println("final energy: " + numBadMatchups);
		
		return toRet;
	}
	
	
	
	private int countInvalidCrossovers(ArrayList<Matchup> matchups)
	{
		HashSet<String> duplicateTracker = new HashSet<String>();
		int cost = 0;
		for (Matchup m : matchups)
		{
			Team homeTeam = m.getHomeTeam();
			Team awayTeam = m.getAwayTeam();
			cost += Team.howBadIsCrossoverGame(homeTeam, awayTeam);
			if (homeTeam.getId() > awayTeam.getId())
			{
				Team tmp = homeTeam;
				homeTeam = awayTeam;
				awayTeam = tmp;
			}
			if (!duplicateTracker.add(homeTeam.getShortIdName() + awayTeam.getShortIdName()))
			{
				++cost;
			}
		}
		return cost;
	}
	
	private ArrayList<Matchup> generateInPoolMatchups(int inPoolGames) {
		ArrayList<Matchup> toRet = new ArrayList<Matchup>();
		ArrayList<ArrayList<Team> > pools = new ArrayList<ArrayList<Team>>();
		
		for (int poolNum = 0; poolNum < 100; ++poolNum)
		{
			ArrayList<Team> teamsInPool = new ArrayList<Team>();
			for (Team team : m_teams)
			{
				if (team.getDivision() == poolNum)
				{
					teamsInPool.add(team);
				}
			}
			if (teamsInPool.size() == 0)
			{
				break;
			}
			pools.add(teamsInPool);
			
			// create in-pool matchups
			toRet.addAll(createMatchupsForTeamGroup(teamsInPool, inPoolGames));
		}
		return toRet;
	}
	
	private ArrayList<Matchup> createMatchupsForTeamGroup(ArrayList<Team> teams, int numGames)
	{
		ArrayList<Matchup> toRet = new ArrayList<Matchup>();
		if (numGames == 0)
		{
			return toRet;
		}
//		ArrayList<Team> circularTeams = new ArrayList<Team>(teams);
//		Team firstTeam = circularTeams.remove(0);
//		int numTeams = teams.size();
//
//		for (int ii = 0; ii < numGames + 1 / 2; ++ii)
//		{
//			toRet.add(new Matchup(firstTeam, circularTeams.get(0)));
//			for (int jj = 1; jj < (numTeams - 1) / 2; ++jj)
//			{
//				if (numGames % 2 == 0 || ii * 2 < numGames || ii % 2 == 0)
//				{
//					int opponentTeamIdx = numTeams - 1 - jj;
//					toRet.add(new Matchup(circularTeams.get(jj), circularTeams.get(opponentTeamIdx)));				
//				}
//			}
//			Team temp = circularTeams.remove(0);
//			circularTeams.add(temp);
//		}
		
		int numTeams = teams.size();
		boolean useVariableOffset = numTeams % 2 == 1 || numGames % 2 == 1;
		for (int ii = 0; ii < numTeams; ++ii)
		{
			Team curTeam = teams.get(ii);
			int offset = numGames / 2;
			if (useVariableOffset && ii % 2 == 1)
			{
				++offset;
			}
			for (int jj = 1; jj <= offset; ++jj)
			{
				Team oppTeam = teams.get((teams.size() + ii - jj) % teams.size());
				Matchup newMatchup = new Matchup(curTeam, oppTeam);
				toRet.add(newMatchup);
			}
		}
		return toRet;
	}

	public ArrayList<Matchup> getMatchups()
	{
		return m_matchups;
	}

	private ArrayList<Team> m_teams;
	private final HashMap<Integer, Team> m_idToTeamLookup = new HashMap<Integer, Team>();
	private final ArrayList<Matchup> m_matchups = new ArrayList<Matchup>();
	
	private class MatchOptMove
	{
		private final int m_m1;
		private final int m_m2;
		private final boolean m_crossTeams;
		private Matchup m_backup1;
		private Matchup m_backup2;
		
		MatchOptMove(int m1, int m2, boolean cross)
		{
			m_m1 = m1;
			m_m2 = m2;
			m_crossTeams = cross;
		}
		
		public void revertMove(ArrayList<Matchup> matchups) {
			matchups.set(m_m1, m_backup1);
			matchups.set(m_m2, m_backup2);
		}

		void applyMove(ArrayList<Matchup> matchups)
		{
			m_backup1 = matchups.get(m_m1);
			m_backup2 = matchups.get(m_m2);
			Matchup new1;
			Matchup new2;
			
			if (m_crossTeams)
			{
				new1 = new Matchup(matchups.get(m_m1).getHomeTeam() , matchups.get(m_m2).getAwayTeam());				
				new2 = new Matchup(matchups.get(m_m1).getAwayTeam() , matchups.get(m_m2).getHomeTeam());
			}
			else
			{
				new1 = new Matchup(matchups.get(m_m1).getHomeTeam() , matchups.get(m_m2).getHomeTeam());				
				new2 = new Matchup(matchups.get(m_m1).getAwayTeam() , matchups.get(m_m2).getAwayTeam());	
			}
			matchups.set(m_m1, new1);
			matchups.set(m_m2, new2);
		}
		
	}
}
