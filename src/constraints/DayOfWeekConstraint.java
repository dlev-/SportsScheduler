package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public class DayOfWeekConstraint extends ScheduleConstraint {

	private static final double S_COST = 400.0;
	private final String[] m_badDaysOfWeek;
	
	public DayOfWeekConstraint(String[] badDaysOfWeek)
	{	
		super(S_COST);
		m_badDaysOfWeek = badDaysOfWeek;
	}

	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		for (GameSlotInfo gsi : gameSlots)
		{
			for (String badDay : m_badDaysOfWeek)
			{
				if (badDay.equalsIgnoreCase(gsi.getGameDay().getDayOfWeek()))
				{
					toRetGsi = gsi;
					++count;
				}
			}
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}
}
