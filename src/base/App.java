package base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;

import constraints.ByeConstraint;


public class App {

	public static int CURRENT_YEAR = 2013;
	
	public static void main(String[] args) throws Exception 
	{
		// do something with parsing strings.
		
		// Options
		// 	  Fetch team file
		//    Generate Matchups
		//    Fetch field space
		//    Create Schedule
		//    Push schedule
		
		String[] fakeArgs = new String[4];
		fakeArgs[0] = "winter14";
		
//		fakeArgs[1] = "fetchTeams";
//		fakeArgs[2] = "http://www.discnw.org/events/Winter/teams";
//		fakeArgs[3] = "defualt";
		
//		fakeArgs[1] = "generateMatchups";
//		fakeArgs[2] = "8";
//		fakeArgs[3] = "0";

//		fakeArgs[1] = "fetchFieldAllotments";
//		fakeArgs[2] = "DiscNW - Winter Team League Game Management.html";
//		fakeArgs[3] = "fieldSchedule.xml";

//		fakeArgs[1] = "createSchdule";
		
//		fakeArgs[1] = "printWorstGames";

//		fakeArgs[1] = "refineSchdule";

//		fakeArgs[1] = "printSchdule";
		
		fakeArgs[1] = "postData";
		
		args = fakeArgs;
		
		if (args.length <= 2)
		{
			PrintUsage();
		}
		
		String workingDirectoryPath = args[0];
		File workingDir = new File(workingDirectoryPath);
		Schedule sched = new Schedule();
		
		if (args[1].equalsIgnoreCase("fetchTeams"))
		{
			if (args.length < 4)
			{
				System.out.println("not enough args");
			}
			if (!workingDir.exists())
			{
				boolean success = workingDir.mkdir();
				if (!success)
				{
					throw new Exception("couldn't create directory: " + workingDirectoryPath);
				}
			}
			boolean applyDefaultCrossovers = !args[3].equalsIgnoreCase("noCrossover");
			
			String xmlLeageFilePath = getFilePath(workingDir, s_leagueTeamPoolXmlFileName);
			if (!new File(xmlLeageFilePath).exists())
			{
				downloadAndTidyWebPage(args[2], xmlLeageFilePath);
			}
			Team.grabFromXml(xmlLeageFilePath, applyDefaultCrossovers, sched.getTeams());
			Team.serializeTeams(sched.getTeams(), getFilePath(workingDir, s_teamPrefsFileName));
		}
		else 
		{
			if (!workingDir.exists())
			{
				throw new Exception("The directory for this league does not exist?");
			}
			
			if (args[1].equalsIgnoreCase("generateMatchups"))
			{
				int numGamesPerTeamInPool = Integer.parseInt(args[2]);
				int numCrossoverGamesPerTeam = Integer.parseInt(args[3]);
				Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), new ArrayList<GameDay>(), null);
				sched.setMatchups(new MatchupSet(sched.getTeams(), numGamesPerTeamInPool, numCrossoverGamesPerTeam));
				sched.getMatchups().serializeMatchups(getFilePath(workingDir, s_matchupsFileName));				
			}
			else if (args[1].equalsIgnoreCase("fetchFieldAllotments"))
			{
				String htmlSchedFileFromDiscNW = getFilePath(workingDir, args[2]);
				String xmlSchedFileToWriteTo = getFilePath(workingDir, args[3]);
				if (!new File(xmlSchedFileToWriteTo).exists())
				{
					String tidyCommandText = "tidy -q -b --output-xml true --doctype omit -o " + xmlSchedFileToWriteTo + " " + htmlSchedFileFromDiscNW;
					runAndWaitForExternalCommand(tidyCommandText);	
				}  
				GameDay.grabFromXml(xmlSchedFileToWriteTo, sched.getGameDays(), sched.getFieldNames());
				GameDay.serializeSchedule(sched.getGameDays(), sched.getFieldNames(), getFilePath(workingDir, s_fieldScheduleFileName));
			}
			else if (args[1].equalsIgnoreCase("createSchdule"))
			{
				int numRuns = 100;
				for (int ii = 0; ii < numRuns; ++ii)
				{
					sched = new Schedule();
					GameDay.deserializeSchedule(getFilePath(workingDir, s_fieldScheduleFileName), sched.getGameDays(), sched.getFieldNames(), null);
					Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), sched.getGameDays(), sched.getFieldNames());
					sched.setMatchups(MatchupSet.deserializeMatchups(sched.getTeams(), getFilePath(workingDir, s_matchupsFileName)));
					sched.applyMatchupsToFields(Scheduler.getRandom());
					Scheduler schedOpter = new Scheduler();

					Schedule optSched = schedOpter.optimizeSchedule(sched);
					optSched = schedOpter.improvingOnlyOptSchedule(optSched);
					double schedEnergy = optSched.getScheduleEnergy();
					String optSchedFileName = s_optScheduleOptionBaseName + "_" + schedEnergy + ".csv";
					GameDay.serializeSchedule(optSched.getGameDays(), optSched.getFieldNames(), getFilePath(workingDir, optSchedFileName));
				}
			}
			else if (args[1].equalsIgnoreCase("refineSchdule"))
			{
				Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), null, null);
				sched.setMatchups(MatchupSet.deserializeMatchups(sched.getTeams(), getFilePath(workingDir, s_matchupsFileName)));
				GameDay.deserializeSchedule(getFilePath(workingDir, s_optScheduleFileName), sched.getGameDays(), sched.getFieldNames(), sched.getMatchups());
				Scheduler schedOpter = new Scheduler();
				sched = schedOpter.improvingOnlyOptSchedule(sched);
				double schedEnergy = sched.getScheduleEnergy();
				String optSchedFileName = s_optScheduleOptionBaseName + "_" + schedEnergy + ".csv";
				sched.writeToConsole();
				GameDay.serializeSchedule(sched.getGameDays(), sched.getFieldNames(), getFilePath(workingDir, optSchedFileName));
			}
			else if (args[1].equalsIgnoreCase("printSchdule"))
			{
				Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), null, null);
				sched.setMatchups(MatchupSet.deserializeMatchups(sched.getTeams(), getFilePath(workingDir, s_matchupsFileName)));
				GameDay.deserializeSchedule(getFilePath(workingDir, s_optScheduleFileName), sched.getGameDays(), sched.getFieldNames(), sched.getMatchups());
				sched.writeToConsole();
			}
			else if (args[1].equalsIgnoreCase("printWorstGames"))
			{
				GameDay.deserializeSchedule(getFilePath(workingDir, s_fieldScheduleFileName), sched.getGameDays(), sched.getFieldNames(), null);
				Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), sched.getGameDays(), sched.getFieldNames());
				sched.setMatchups(MatchupSet.deserializeMatchups(sched.getTeams(), getFilePath(workingDir, s_matchupsFileName)));
				GameDay.deserializeSchedule(getFilePath(workingDir, s_optScheduleFileName), sched.getGameDays(), sched.getFieldNames(), sched.getMatchups());
				for (Team team : sched.getTeams())
				{
					if (team.getCost() > ByeConstraint.S_COST)
					{
						GameSlotInfo worstGSI = team.getWorstGsi();
						if (worstGSI != null)
						{
							System.out.println(team.getName() + "has bad game: " + worstGSI.toString());
						}
					}
				}
			}
			else if (args[1].equalsIgnoreCase("postData"))
			{
				Team.deserializeTeams(getFilePath(workingDir, s_teamPrefsFileName), sched.getTeams(), null, null);
				sched.setMatchups(MatchupSet.deserializeMatchups(sched.getTeams(), getFilePath(workingDir, s_matchupsFileName)));
				GameDay.deserializeSchedule(getFilePath(workingDir, s_optScheduleFileName), sched.getGameDays(), sched.getFieldNames(), sched.getMatchups());
				writePostData(getFilePath(workingDir, s_postDataFileName), sched.getGameDays());
				sched.writeToConsole();
			}
			else
			{
				System.out.println("Unknown input...");
				PrintUsage();
			}
		}
		
		System.out.println("Done with " + args[1] + " in dir " + args[0]);
	
	}
	
	private static void writePostData(String filePath, ArrayList<GameDay> gameDays) throws IOException 
	{
		final String valSeperator = "&";
		final String valSetter = "=";
		StringBuilder sb = new StringBuilder();
		sb.append("week_selector" + valSetter + "all" + valSeperator);
		for (GameDay gd : gameDays)
		{
			for (GameTime gt : gd.getGameTimes())
			{
				for (Game g : gt.getGames())
				{
					int webId = g.getWebGameId();
					if (webId >= 0)
					{
						String gameLine = "gameId_" + webId + valSetter + "0";
						sb.append(gameLine + valSeperator);
						String schedLine = "scheduleId_" + webId + valSetter + g.getWebGameValue();
						sb.append(schedLine + valSeperator);

						String team1Line = "team1_" + webId + valSetter;
						String team2Line = "team2_" + webId + valSetter;
						if (g.getMatchup() != null)
						{
							team1Line = "team1_" + webId + valSetter + g.getMatchup().getHomeTeam().getId();
							team2Line = "team2_" + webId + valSetter + g.getMatchup().getAwayTeam().getId();
						}
						sb.append(team1Line + valSeperator);
						sb.append(team2Line + valSeperator);
					}
				}
			}
		}
		sb.append("saveGames" + valSetter + "Save+Game+Data\n\n\n");
		
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
//		bw.write("Content-Length: " + sb.length());
//		bw.newLine();
//		bw.newLine();
		bw.write(sb.toString());
		bw.close();
	}

	private static String getFilePath(File workingDir, String fileName)
	{
		fileName = fileName.replaceAll(" ", "\\\\ ");
		return new File(workingDir, fileName).getPath();
	}
	
	private static void downloadAndTidyWebPage(String url, String filePath) throws IOException, InterruptedException
	{
		String tempHtmlFileName = "temp.html";
		String wgetCommandText = "wget -O "  + tempHtmlFileName + " " + url;
		String tidyCommandText = "tidy -q -b --output-xml true --doctype omit -o " + filePath + " " + tempHtmlFileName;
		runAndWaitForExternalCommand(wgetCommandText);
		runAndWaitForExternalCommand(tidyCommandText);	
	}
	
	private static void runAndWaitForExternalCommand(String command) throws IOException, InterruptedException
	{
		System.out.println("Running: " + command);
		Process proc = Runtime.getRuntime().exec(PATH_ENV_VALUE + command, new String[] {"PATH=" + PATH_ENV_VALUE});
		proc.waitFor();
	}

	private static void PrintUsage() {
		System.out.println(
			"usage: workingDir <mode> <options> \n" +
			"Modes \n" +
			"fetchTeam - fetches the teams and pools from the given url (in options). The second option is 'noCrossover' or 'defualt' \n" +
			"generateMatchups - uses the teams file to generate matchups. The number of games per team is the option parameter.\n"
		);
		System.exit(0);
	}
	
	private final static String s_leagueTeamPoolXmlFileName = "leagueTeams.xml";
	private final static String s_teamPrefsFileName = "teams.csv";
	private final static String s_matchupsFileName = "matchups.csv";
	private final static String s_fieldScheduleFileName = "fields.csv";
	private final static String s_optScheduleOptionBaseName = "optSched";
	private final static String s_optScheduleFileName = "optSched.csv";
	private final static String s_postDataFileName = "postData.txt";
	private static String PATH_ENV_VALUE = "/opt/local/bin/";

}
