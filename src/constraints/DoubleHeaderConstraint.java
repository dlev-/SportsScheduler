package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;
import base.Schedule;

public class DoubleHeaderConstraint extends ScheduleConstraint {

	private static final double S_COST = 1000.0;
	private final int m_minGamesInRow;
	private final int m_maxGamesInRow;
	private ArrayList<String> m_fieldNames;
	
	public DoubleHeaderConstraint(int minInRow, int maxInRow)
	{	
		super(S_COST);
		assert(minInRow <= maxInRow);
		m_minGamesInRow = minInRow;
		m_maxGamesInRow = maxInRow;
		if (m_minGamesInRow < 1) throw new Error("min in a row needs to be >= 1");
	}
	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		ArrayList<GameSlotInfo> gamesOnDay = new ArrayList<GameSlotInfo>();
		gamesOnDay.add(gameSlots.get(0));
		for (int ii = 1; ii < gameSlots.size(); ++ii)
		{
			GameSlotInfo gsi = gameSlots.get(ii);
			GameSlotInfo lastGsi = gamesOnDay.get(gamesOnDay.size() - 1);
			if (lastGsi.getGameDay().getDateString().equals(gsi.getGameDay().getDateString()) &&
					areNeighboringFields(gsi, lastGsi) &&
				Schedule.S_GAME_HOUR_DURATION == Math.abs(gsi.getGameTime().getHour() - lastGsi.getGameTime().getHour()))
			{
				// do nothing
			}
			else
			{
				int gamesInARow = gamesOnDay.size();
				if (!(m_minGamesInRow <= gamesInARow && gamesInARow <= m_maxGamesInRow))
				{
					toRetGsi = gsi;
					++count;
				}
				gamesOnDay.clear();
			}
			gamesOnDay.add(gsi);
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}

	public void setFieldNames(ArrayList<String> fieldNames) 
	{
		m_fieldNames = fieldNames;	
	}
	
	private boolean areNeighboringFields(GameSlotInfo gsi1, GameSlotInfo gsi2)
	{
		assert(m_fieldNames != null);
		String field1 = getFieldName(gsi1);
		String field2 = getFieldName(gsi2);
		return field1.equals(field2);
	}
	
	private String getFieldName(GameSlotInfo gsi)
	{
		return stripLastChar(m_fieldNames.get(gsi.getGame().getFieldNum()));
	}
	
	private String stripLastChar(String s)
	{
		s = s.trim();
		return s.substring(0, s.length() - 1);
	}
}
