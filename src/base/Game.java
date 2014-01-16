package base;


public class Game {

	private final int m_fieldNum;
	private Matchup m_matchup = null;
	private int m_webGameId;
	private int m_webGameValue;

	public Game(int fieldNum, int webGameId, int webGameVal) {
		m_fieldNum = fieldNum;
		m_webGameId = webGameId;
		m_webGameValue = webGameVal;
	}

	public Matchup getMatchup() {
		return m_matchup;
	}

	public int getFieldNum() {
		return m_fieldNum;
	}
	
	public int getWebGameId() {
		return m_webGameId;
	}

	public int getWebGameValue() {
		return m_webGameValue;
	}

	public void setMatchup(Matchup m) {
		m_matchup = m;
	}

	public boolean isFieldUsed() {
		return m_matchup != null;
	}
	
	public String getSerializeString()
	{
		String toRet = "free_" + m_fieldNum;
		if (m_matchup != null)
		{
			toRet = m_matchup.getHomeTeam().getShortIdName() + 
					" " +
					m_matchup.getAwayTeam().getShortIdName();
		}
		return toRet;
	}
	
	public String toString()
	{
		return getSerializeString();
	}
}
