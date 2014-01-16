package base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


public class Schedule 
{	
	private static final Random s_rand = Scheduler.getRandom();
	private static final boolean S_EXTRA_DEBUG = false;
	private double m_cachedEnergy = -1;
	
	private final ArrayList<String> m_fieldNames;
	private final ArrayList<GameDay> m_gameDays = new ArrayList<GameDay>();
	private final ArrayList<Team> m_teams;
	private ArrayList<Integer> m_teamIndexes;
	private MatchupSet m_matchups;

	public final static int S_GAME_HOUR_DURATION = 2;
	
	public Schedule()
	{
		m_fieldNames = new ArrayList<String>();
		m_teams = new ArrayList<Team>();
		invalidateCache();
	}
	
	
	private void invalidateCache()
	{
		m_cachedEnergy = -1;
	}
	
	public void applyMatchupsToFields(Random rand)
	{
		ArrayList<Matchup> matchups = new ArrayList<Matchup>(m_matchups.getMatchups());
		Collections.shuffle(matchups, rand);
		int unusedFields = 0;
		
		for (GameDay gd : m_gameDays)
		{
			for (GameTime gt : gd.getGameTimes())
			{
				for (Game g : gt.getGames())
				{
					if (matchups.size() > 0)
					{
						Matchup matchup = matchups.remove(matchups.size() - 1);
						g.setMatchup(matchup);
					}
					else
					{
						++unusedFields;
					}
				}
			}
		}
		if (matchups.size() != 0) throw new Error("Too many matchups for the number of fields");
		System.out.println("Number of unused fields: " + unusedFields);
		initTeamGameSlots();
	}
	
	private void initTeamGameSlots()
	{
		for (Team team : m_teams)
		{
			team.clearGameSlots();
		}
		
		for (GameDay gd : m_gameDays)
		{
			for (GameTime gt : gd.getGameTimes())
			{
				for (Game g : gt.getGames())
				{
					if (g.isFieldUsed())
					{
						GameSlotInfo gsi = new GameSlotInfo(gd, gt, g);
						g.getMatchup().getHomeTeam().addGameSlot(gsi);
						g.getMatchup().getAwayTeam().addGameSlot(gsi);
					}
				}
			}
		}
		
		for (Team team : m_teams)
		{
			team.prepareForOpt(m_fieldNames);
		}
	}
	
	
	public double getScheduleEnergy()
	{
		if (m_cachedEnergy < 0)
		{
			calcScheduleEnergy();
		}
		return m_cachedEnergy;
	}
	private void calcScheduleEnergy()
	{
		double cost = 0;
		for (Team team : m_teams)
		{
			cost += team.getCost();
		}
		m_cachedEnergy = cost;
	}
	
	private void verifyGameSlots()
	{
		assert(S_EXTRA_DEBUG);
		for (GameDay gd : m_gameDays)
		{
			for (GameTime gt : gd.getGameTimes())
			{
				for (Game g : gt.getGames())
				{
					if (g.isFieldUsed())
					{
						GameSlotInfo gsi = new GameSlotInfo(gd, gt, g);
						g.getMatchup().getHomeTeam().verifyTeamHasGsi(gsi);
						g.getMatchup().getAwayTeam().verifyTeamHasGsi(gsi);
					}
				}
			}
		}				
	}
	
	public void applyScheduleChange(ScheduleChange sc)
	{
		if (S_EXTRA_DEBUG) { verifyGameSlots(); }
		GameSlotInfo oldGsi = sc.getOriginalGsi();
		GameSlotInfo newGsi = sc.getNewGsi();

		swapGsisOnTeams(oldGsi, newGsi);
		swapGsisOnTeams(newGsi, oldGsi);
		
		Matchup oldMatchup = oldGsi.getGame().getMatchup();
		Matchup newMatchup = newGsi.getGame().getMatchup();
		oldGsi.getGame().setMatchup(newMatchup);
		newGsi.getGame().setMatchup(oldMatchup);	
		
		if (S_EXTRA_DEBUG) 
		{ 
			if (oldMatchup != null)
			{
				oldMatchup.getHomeTeam().verifyTeamHasGsi(newGsi);
				oldMatchup.getAwayTeam().verifyTeamHasGsi(newGsi);
			}
	
			if (newMatchup != null)
			{
				newMatchup.getHomeTeam().verifyTeamHasGsi(oldGsi);
				newMatchup.getAwayTeam().verifyTeamHasGsi(oldGsi);
			}
			verifyGameSlots(); 
		}
		
		invalidateCache();
	}
	
	private void swapGsisOnTeams(GameSlotInfo gsi1, GameSlotInfo gsi2)
	{
		Matchup matchup = gsi1.getGame().getMatchup();
		if (matchup != null)
		{
			matchup.getHomeTeam().replaceGameSlot(gsi1, gsi2);
			matchup.getAwayTeam().replaceGameSlot(gsi1, gsi2);
		}
	}
	

	private GameSlotInfo selectRandomGameSlot()
	{
		int gameDayIdx = s_rand.nextInt(m_gameDays.size());
		return selectRandomGameSlotOnDay(m_gameDays.get(gameDayIdx));
	}

	private GameSlotInfo selectRandomGameSlotOnDay(GameDay gd)
	{
		int gameTimeIdx = s_rand.nextInt(gd.getGameTimes().size());
		GameTime gt = gd.getGameTimes().get(gameTimeIdx);
		int fieldNumIdx = s_rand.nextInt(gt.getGames().size());
		Game game = gt.getGames().get(fieldNumIdx);
		GameSlotInfo toRet = new GameSlotInfo(gd, gt, game);
		return toRet;
	}

	
	public ScheduleChange generateRandomMove() 
	{
		GameSlotInfo firstGameSlot = selectRandomGameSlot();
		while (!firstGameSlot.getGame().isFieldUsed())
		{
			firstGameSlot = selectRandomGameSlot();
		}
		GameSlotInfo secondGameSlot = selectRandomGameSlot();
		ScheduleChange toRet = new ScheduleChange(firstGameSlot, secondGameSlot);
		
		return toRet;
	}

	public ScheduleChange generateImprovingMove() 
	{
		double curEnergy = getScheduleEnergy();
		ScheduleChange toRet = null;
		int maxIters= 100;
		GameSlotInfo badSlot = getBadGameSlot();
//		GameSlotInfo badSlot = selectRandomGameSlot();
		if (badSlot != null)
		{
			for (int ii = 0; ii < maxIters; ++ii)
			{
				GameSlotInfo otherSlot = selectRandomGameSlot();
//				GameSlotInfo otherSlot = selectRandomGameSlotOnDay(badSlot.getGameDay());
//				GameSlotInfo otherSlot = ii < maxIters / 2 ? 
//							selectRandomGameSlot() :
//							selectRandomGameSlotOnDay(m_gameDays.indexOf(badSlot.getGameDay()));
						    
				ScheduleChange possibleChange = new ScheduleChange(badSlot, otherSlot);
				applyScheduleChange(possibleChange);
				double newEnergy = getScheduleEnergy();
				if (newEnergy < curEnergy)
				{
					toRet = possibleChange;
					curEnergy = newEnergy;
				}
				ScheduleChange undoChange = possibleChange.reverseChange();
				applyScheduleChange(undoChange);
			}
		}
		return toRet;
	}

	public ScheduleChange generateImprovingMove2() 
	{
		GameSlotInfo badSlot = getBadGameSlot();
		return generateImprovingMove2(badSlot);
	}
	
	public ScheduleChange generateImprovingMove2(GameSlotInfo badSlot)
	{
		ScheduleChange toRet = null;
		if (badSlot != null)
		{
			double curEnergy = getScheduleEnergy();
			for (GameDay gd : m_gameDays)
			{
				for(GameTime gt : gd.getGameTimes())
				{
					for (Game game : gt.getGames())
					{
						GameSlotInfo potentialSwap = new GameSlotInfo(gd, gt, game);
						ScheduleChange possibleChange = new ScheduleChange(badSlot, potentialSwap);
						applyScheduleChange(possibleChange);
						double newEnergy = getScheduleEnergy();
						if (newEnergy < curEnergy)
						{
							toRet = possibleChange;
							curEnergy = newEnergy;
						}
						ScheduleChange undoChange = possibleChange.reverseChange();
						applyScheduleChange(undoChange);
					}
				}
			}
		}
		return toRet;
		
	}
	
	
	public ScheduleChange improveTeamSched(Team team)
	{
		double curEnergy = getScheduleEnergy();
		GameSlotInfo badSlot = team.getWorstGsi();
		if (badSlot != null)
		{
			for (GameTime gt : badSlot.getGameDay().getGameTimes())
			{
				for (Game game : gt.getGames())
				{
					GameSlotInfo otherSlot = new GameSlotInfo(badSlot.getGameDay(), gt, game);
					
					ScheduleChange possibleChange = new ScheduleChange(badSlot, otherSlot);
					applyScheduleChange(possibleChange);
					double newEnergy = getScheduleEnergy();
					ScheduleChange undoChange = possibleChange.reverseChange();
					applyScheduleChange(undoChange);
					if (newEnergy < curEnergy)
					{
						return possibleChange;
					}
				}
			}			
		}
		return null;
	}
	
	

	private GameSlotInfo getBadGameSlot()
	{
		GameSlotInfo toRet = null;
		double worstCost = 0;
		int numTeams = m_teams.size();
		
		if (m_teamIndexes == null || m_teamIndexes.size() != numTeams)
		{
			m_teamIndexes = new ArrayList<Integer>(numTeams);
			for (int ii = 0; ii < numTeams; ++ii)
			{
				m_teamIndexes.add(ii);
			}
		}
		Collections.shuffle(m_teamIndexes);
		
		for (int ii = 0; ii < numTeams; ++ii)
		{
			Team team = m_teams.get(ii);
			if (team.getCost() > worstCost || 
				(team.getCost() == worstCost && s_rand.nextDouble() < 0.3))
			{
				worstCost = team.getCost();
				toRet = team.getWorstGsi();
			}
		}
		return toRet;
	}


	public ArrayList<Team> getTeams() {
		return m_teams;
	}

	public MatchupSet getMatchups() {
		return m_matchups;
	}

	public void setMatchups(MatchupSet mMatchups) {
		m_matchups = mMatchups;
	}

	public ArrayList<String> getFieldNames() {
		return m_fieldNames;
	}

	public ArrayList<GameDay> getGameDays() {
		return m_gameDays;
	}
	
	public void writeToConsole()
	{
		for(Team team : m_teams)
		{
			System.out.println(team.getName() + "   Byes: " + team.getByeDates());
			ArrayList<String> schedLines = new ArrayList<String>();
			String[] byeDates = team.getByeDates().split(" ");
			for (GameDay gd : m_gameDays)
			{
				for (GameTime gt : gd.getGameTimes())
				{
					for (Game g : gt.getGames())
					{
						if (g.isFieldUsed() && g.getMatchup().isTeamInMatchup(team))
						{
							Team opponent = g.getMatchup().getHomeTeam();
							if (opponent.equals(team))
							{
								opponent = g.getMatchup().getAwayTeam();
							}
							String schedLine = gd.getDateString() + "\t" +
							                   gt.getTimeString() + "\t" +
							                   m_fieldNames.get(g.getFieldNum()) + "\t\t" + 
							                   opponent.getName();
							for (String date : byeDates)
							{
								if (date.equals(gd.getDateString()))
								{
									schedLine += "   <<<---- BAD BYE!!!";
								}
									
							}
							schedLines.add(schedLine);
						}
					}
				}
			}
			Collections.sort(schedLines);

			for (String line : schedLines)
			{
				System.out.println("\t" + line);				
			}				
		}
	}
}
