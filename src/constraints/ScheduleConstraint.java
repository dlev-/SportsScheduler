package constraints;

import java.util.ArrayList;

import util.Pair;

import base.GameSlotInfo;

public abstract class ScheduleConstraint implements Comparable<ScheduleConstraint>
{
	private final double m_cost;
	
	protected ScheduleConstraint(double cost)
	{
		m_cost = cost;
	}
	
	public double getCost()
	{
		return m_cost;
	}


	public int compareTo(ScheduleConstraint o) {
		return m_cost < o.m_cost ? 1 : -1;
	}

	public abstract Pair<GameSlotInfo, Double> eval(ArrayList<GameSlotInfo> gameSlots);

}
