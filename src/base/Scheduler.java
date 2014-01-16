package base;
import java.util.*;


public class Scheduler {
		
	private static final int MAX_SA_ITERATIONS = 10000;
		
	private final static Random S_RANDOM = new Random();
	public static Random getRandom()
	{
		return S_RANDOM;
	}

	public Schedule improvingOnlyOptSchedule(final Schedule sched) 
	{
		double lastEnergy = sched.getScheduleEnergy() * 1.1;
		for (int iteration = 0; iteration < MAX_SA_ITERATIONS && sched.getScheduleEnergy() < lastEnergy; ++iteration)
		{
			lastEnergy = sched.getScheduleEnergy();
			
			for (Team team : sched.getTeams()) 
			{
				GameSlotInfo badGsi = team.getWorstGsi();
				ScheduleChange schedChange = sched.generateImprovingMove2(badGsi);
				if (schedChange != null)
				{
					sched.applyScheduleChange(schedChange);
				}
			}
			
			if (iteration % 20 == 0)
			{
				System.out.println(iteration + ": " + sched.getScheduleEnergy());
			}
		}	
		System.out.println("final energy: " + sched.getScheduleEnergy());
		return sched;
	}
	
	public Schedule optimizeSchedule(final Schedule sched) 
	{
		Random rand = getRandom();

		for (int iteration = 0; iteration < MAX_SA_ITERATIONS && sched.getScheduleEnergy() > 0; ++iteration)
		{
			double randStepCutoff = (double)(MAX_SA_ITERATIONS - iteration) / (double)(MAX_SA_ITERATIONS);
			ScheduleChange schedChange = null;
			boolean performRandMove = rand.nextDouble() < randStepCutoff;
			if (performRandMove)
			{
				//perform random step
				schedChange = sched.generateRandomMove();
			} 
			else
			{
				// perform hillclimbing step
				schedChange = sched.generateImprovingMove2();
			}
//			schedChange = sched.generateImprovingMove();
//			schedChange = sched.generateRandomMove();
			
			if (schedChange != null)
			{
				sched.applyScheduleChange(schedChange);
			}
			
			if (iteration %1000 == 0)
			{
				System.out.println(iteration + ": " + sched.getScheduleEnergy());
			}
		}
		
		System.out.println("final energy: " + sched.getScheduleEnergy());
		return sched;
	}
}
