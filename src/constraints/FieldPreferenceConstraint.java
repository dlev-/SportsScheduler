package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public class FieldPreferenceConstraint extends ScheduleConstraint {
	private static final double S_COST = 100.0;
	private final int[] m_fieldIds;
	
	public FieldPreferenceConstraint(ArrayList<Integer> fieldIds)
	{	
		super(S_COST);
		m_fieldIds = new int[fieldIds.size()];
		for (int ii = 0; ii < fieldIds.size(); ++ii)
		{
			m_fieldIds[ii] = fieldIds.get(ii);
		}
	}

	
	@Override
	public Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots) 
	{
		int count = 0;
		GameSlotInfo toRetGsi = null;
		for (GameSlotInfo gsi : gameSlots)
		{
			boolean approvedField = false;
			for (int fieldId : m_fieldIds)
			{
				if (gsi.getGame().getFieldNum() == fieldId)
				{
					approvedField = true;
				}
			}
			if (! approvedField)
			{
				toRetGsi = gsi;
				++count;
			}
		}
		return new Pair<GameSlotInfo, Double>(toRetGsi, count * getCost());
	}
}
