package base;
import java.util.ArrayList;


public class GameTime {

	// 24 hour clock
	private final int m_gameHour;
	private final int m_gameMinute; // probably either 0 or 30
	private final ArrayList<Game> m_games = new ArrayList<Game>();
	

	public GameTime(String gameTimeText) 
	{
		String[] timeParts = gameTimeText.trim().split(":");
		m_gameHour = Integer.parseInt(timeParts[0]);
		m_gameMinute = Integer.parseInt(timeParts[1]);
	}

	public void addGame(Game newGame) {
		m_games.add(newGame);
	}

	public String getTimeString() {
		return String.format("%d:%d", m_gameHour, m_gameMinute);
	}

	public ArrayList<Game> getGames() {
		return m_games;
	}

	public int getHour() {
		return m_gameHour;
	}
}
