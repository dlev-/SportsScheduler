package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;
import base.Schedule;

public class NonConsecutiveGameConstraint extends ScheduleConstraint {

	private static final double S_COST = 10000000.0;
	
	public NonConsecutiveGameConstraint()
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
				lastGsi.getGameDay().getDateString().equals(gsi.getGameDay().getDateString()))
			{
				if (gsi.getGame().getFieldNum() != lastGsi.getGame().getFieldNum() ||
					Schedule.S_GAME_HOUR_DURATION != Math.abs(gsi.getGameTime().getHour() - lastGsi.getGameTime().getHour()))
				{
					toRetGsi = gsi;
					++count;
				}
			}
			lastGsi = gsi;
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}

}
