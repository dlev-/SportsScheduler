package base;


public class ScheduleChange 
{
	private final GameSlotInfo m_oGsi;
	private final GameSlotInfo m_nGsi;
	
	public ScheduleChange(GameSlotInfo oGsi, GameSlotInfo nGsi)
	{
		m_oGsi = oGsi;
		m_nGsi = nGsi;
	}
	
	public ScheduleChange reverseChange()
	{
		return new ScheduleChange(m_nGsi, m_oGsi);
	}

	public GameSlotInfo getOriginalGsi() 
	{
		return m_oGsi;
	}

	public GameSlotInfo getNewGsi() 
	{
		return m_nGsi;
	}
}
