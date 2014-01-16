package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public class OneGamePerWeekConstraint extends ScheduleConstraint {

	private static final double S_COST = 100.0;
//	private static final int S_MIN_DAYS_BETWEEN_GAMES = 10;
//	private static final long S_MILS_PER_DAY = 24 * 60 * 60 * 1000;
	
	public OneGamePerWeekConstraint()
	{	
		super(S_COST);
	}
	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		GameSlotInfo lastGsi = null;
		for (GameSlotInfo gsi : gameSlots)
		{
			if (lastGsi != null &&
				lastGsi.getGameDay().getWeekId().equals(gsi.getGameDay().getWeekId()))
			{
//				long milsDif = gsi.getGameDay().getDate().getTimeInMillis() - lastGsi.getGameDay().getDate().getTimeInMillis();
//				int days = (int)(milsDif / S_MILS_PER_DAY);
//				int overDays = days - S_MIN_DAYS_BETWEEN_GAMES;
//				if (overDays > 0)
//				{
//					toRetGsi = gsi;
//					count += overDays;					
//				}

				toRetGsi = gsi;
				++count;
			}
			lastGsi = gsi;
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}
}
