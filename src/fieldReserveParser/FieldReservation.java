package fieldReserveParser;

public class FieldReservation {
	private final String m_fieldName;
	private final String m_date;
	private double m_startTime;
	private double m_endTime;

	public FieldReservation(String fieldName, String date, double startTime, double endTime)
	{
		m_fieldName = fieldName;
		m_date = date;
		m_startTime = startTime;
		m_endTime = endTime;
	}
	
	/**
	 * Returns true if the reservation was absorbed by this
	 * @param otherRes
	 * @return
	 */
	public boolean absorbReservation(FieldReservation otherRes)
	{
		boolean toRet = false;
		if (m_fieldName.equals(otherRes.m_fieldName) && 
			m_date.equals(otherRes.m_date))
		{
			if (m_endTime == otherRes.m_startTime)
			{
				m_endTime = otherRes.m_endTime;
				toRet = true;
			}
			if (m_startTime == otherRes.m_endTime)
			{
				m_startTime = otherRes.m_startTime;
				toRet = true;
			}
		}
		return toRet;
	}

	
	public String getFieldName() {
		return m_fieldName;
	}

	public String getDate() {
		return m_date;
	}

	public double getStartTime() {
		return m_startTime;
	}

	public double getEndTime() {
		return m_endTime;
	}
	
	public String toString()
	{
		return getDate() + " " + getFieldName() + ": " + getStartTime() + "-" + getEndTime();
	}
}
