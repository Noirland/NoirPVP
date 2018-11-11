# NoirPVP
PVP management plugin for Noirland 2018

### Using NoirPVP:

#### Player Commands:

Voting in trials:
Players may vote in a murder-triggered trial using either /innocent or /guilty
 
Voting in admin-initiated trials: Players may vote in admin trials using
/innocent, /njail, /nkick or /nban

#### Admin Commands:

Use /njail \<playername\> \<reason\> to initiate a trial as discipline for a player.
A reason must be provided.

Use /njail addcell to add a jail cell. The first jail cell can be populated by as many
players as needed, subsequently added cells will all contain a single jailed player.

Use /setdock to set the "Courthouse dock", the place where players are teleported to when
they are put on trial.

Use /setrelease to set the point where players are released after serving time or being found innocent.
This defaults to the world spawn - I can change that later.

#### Permissions:

 - noirpvp.vote: allows players to vote in trials. Jailed players are automatically blocked from voting by the plugin
 - noirpvp.jail: allows admins to initiate discipline trials (use /njail \<playername> \<reason>)
 - noirpvp.setlocations: allows admins to add locations for jail cells, the dock and the release point
 
#### Notes:

Currently it's not possible to remove jail locations other than by editing the yml file
 (although the dock and release point can be overridden)
### Changelog:

0.1.91:
  - The plugin now stops jailed players from modifying their inventory

0.1.9:
  - Trials now resume after server stops during a trial.

0.1.8:
  - Fixed bugs

0.1.7:
  - Made more stuff persistent / made jail time resume

0.1.6:
  - Added Jails, the Courthouse Dock, and release point.
  - Made the defendant teleport to key locations upon trials 
  starting and ending
  - Fixed weapons losing durability when hitting players and pvp gets cancelled

0.1.5:
  - Added a database, made state persistent.
  - PVP cooldowns now take into account the user logging off

0.1.4:
  - Added a config.yml

0.1.3:
  - put the trial scheduling system in place
  
0.1.1:
  - got PVP cooldowns working
  
0.0.1
  - project skeleton and blocking of PVP in claims
