SportsScheduler
===============

Sports league scheduling tool

Problem: Need to schedule a sports league season of games
Inputs: 
- Teams (broken up into divisions) with preferences like byes
- Field reservations

Output:
A schedule of games that adheer to the team preferneces as best as possbile, and avoid problems like a team playing on two different fields at the same time, or a team playing twice in a day at non-consecutive times.

Currently the tool is focused on discnw.org, but could be adapted for other scheduling needs. The final output is structured to easily push the schedule to discnw.org as a league admin.


To run the code, check you App.java in src/base/ You need to comment in/out various options in there. There are some examples already in there... it's pretty rough.
