package constraints;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import util.Pair;

import base.GameDay;
import base.GameSlotInfo;
import base.Team;

public class TimeBetweenRematchConstraint extends ScheduleConstraint {

	private static final double S_COST = 80.0;
	private static final int S_MIN_DAYS_BETWEEN_REMATCH = 21;
	private final HashSet<Team> m_rematchOpponents;
	
	public TimeBetweenRematchConstraint(HashSet<Team> rematchOpponents)
	{	
		super(S_COST);
		m_rematchOpponents = rematchOpponents;
	}
	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		for (Team opTeam : m_rematchOpponents)
		{
			GameDay prevGameDay = null;
			for (GameSlotInfo gsi : gameSlots)
			{
				if (gsi.getGame().getMatchup().isTeamInMatchup(opTeam))
				{
					if (prevGameDay != null)
					{
						int daysBetweenRematch = daysBetween(prevGameDay.getDate(), gsi.getGameDay().getDate());
						
						if (daysBetweenRematch < S_MIN_DAYS_BETWEEN_REMATCH)
						{
							toRetGsi = gsi;
							++count;
						}
					}
					
					prevGameDay = gsi.getGameDay();
				}
			}
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}
	
	public static int daysBetween(Calendar startDate, Calendar endDate) 
	{  
		assert(startDate.equals(endDate) || startDate.before(endDate));
		Calendar date = (Calendar) startDate.clone();  
		int daysBetween = 0;  
		while (date.before(endDate)) 
		{  
			date.add(Calendar.DAY_OF_MONTH, 1);  
		    daysBetween++;  
		}  
		return daysBetween;    
	}  
}
