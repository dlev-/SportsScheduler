package base;

public class Matchup {
	private final Team m_homeTeam;
	private final Team m_awayTeam;
	
	public Matchup(Team h, Team a)
	{
		m_homeTeam = h;
		m_awayTeam = a;
	}
	
	public Team getHomeTeam()
	{
		return m_homeTeam;
	}
	public Team getAwayTeam()
	{
		return m_awayTeam;
	}
	public Team[] getTeams()
	{
		return new Team[] {getHomeTeam(), getAwayTeam()};
	}
	public boolean isTeamInMatchup(Team t)
	{
		return t.equals(m_homeTeam) || t.equals(m_awayTeam);
	}
	
	public static boolean Equals(Matchup m1, Matchup m2)
	{
		return (m1 == null && m2 == null) ||
			   (m1 != null && m1.equals(m2));
	}
	
	public String toString()
	{
		return m_homeTeam.getShortIdName() + " v " + m_awayTeam.getShortIdName();
	}
}
