package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public class TimeOfDayConstraint extends ScheduleConstraint {

	private static final double S_COST = 200.0;
	private final int m_minHour;
	private final int m_maxHour;
	
	public TimeOfDayConstraint(int minHour, int maxHour)
	{	
		super(S_COST);
		m_minHour = minHour;
		m_maxHour = maxHour;
	}
	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		for (GameSlotInfo gsi : gameSlots)
		{
			int gameHour = gsi.getGameTime().getHour();
			if (!(m_minHour < 0 || m_minHour <= gameHour) &&
				 (m_maxHour < 0 || m_maxHour >= gameHour))
			{
				toRetGsi = gsi;
				++count;
			}
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}
}
