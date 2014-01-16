package base;

public class GameSlotInfo {

	
	private final GameDay m_gd;
	private final GameTime m_gt;
	private final Game m_g;

	public GameSlotInfo(GameDay gd, GameTime gt, Game g)
	{
		m_gd = gd;
		m_gt = gt;
		m_g = g;
	}

	public GameDay getGameDay() {
		return m_gd;
	}

	public GameTime getGameTime() {
		return m_gt;
	}

	public Game getGame() {
		return m_g;
	}

	public String toString()
	{
		return m_gd.getDateString() + " " + m_gt.getTimeString() + " on " + m_g.getFieldNum() + ": " + m_g.toString();
	}

	@Override
	public boolean equals(Object obj) {	
		boolean toRet = obj instanceof GameSlotInfo;
		if (toRet)
		{
			GameSlotInfo oGsi = (GameSlotInfo) obj;
			toRet = m_g.getFieldNum() == oGsi.m_g.getFieldNum() &&
				    m_gt.getHour() == oGsi.m_gt.getHour() &&
				    m_gd.getDateString().equals(oGsi.m_gd.getDateString());
		}
		return toRet;
	}
	


}
