package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public class ByeConstraint extends ScheduleConstraint {

	public static final double S_COST = 10000.0;
	private final String[] m_dateStrings;
	
	public ByeConstraint(String[] byeDateStrings)
	{	
		super(S_COST);
		m_dateStrings = byeDateStrings;
	}
	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;		
		for (GameSlotInfo gsi : gameSlots)
		{
			for (String byeDate : m_dateStrings)
			{
				if (byeDate.equalsIgnoreCase(gsi.getGameDay().getDateString()))
				{
					toRetGsi = gsi;
					++count;
				}
			}
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}

}
