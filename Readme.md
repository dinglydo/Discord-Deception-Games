# Discord Deception Games (DDG)
This is a Town of Salem inspired discord bot. This will become a collection of party games that fundamentally have more or less the same rules, but will vary by the roles that will be available. We're just finishing up **Talking Graves**, which has the roles:
- Civilian
- Medium
- Lookout
- Serial Killer

The number of each role that can exist will differ depending on the party size, but generally speaking Civilian is the most common role and Medium is the least common (Only one per game that isn't random). This game mode was made specifically because of the nice combination that a Medium and a Lookout can make. Because I lookout can see who visits him overnight, a Medium would be vital for seeing who killed the lookout. And since there can only be one Medium at most (if it isn't random), conflicts could arise on who's the medium and who's the lookout.

We also have another game mode called Mashup, which simply combines all the currently existing roles, but it's not ready for release yet.

## Tutorial
Assuming you've invited the bot to your server, instructions on that later, you can view most of the commands with `pg.help`, but here's the run of the mill walkthrough on how to start a game:
- `pg.startparty` -> starts the lobby, there can only be one lobby in a server. (The one who starts the lobby becomes the party leader)
- `pg.join` -> joins the current lobby in the server. Note: if you started the party, you automatically join.
- `pg.party` -> to check who's currently in the lobby. Can also be used in the game.
- `pg.startgame` -> starts the game. Everyone in the lobby should get an invite to a new server. Only the party leader can start a game.
- `pg.endparty` -> if you want to remove the party and not start the game. Note: This is automatically done when starting a game.
(This command can be activated by anybody. This is done in the case that the party leader is AFK)

Here are commands used once you are in the game (The pg. prefix can be replaced with ! once in the game):
- `!ability (mention|num)` (or `!a`) -> Uses your role ability. Some roles don't have one, others require a parameter.
- `!targets` -> Gives you a list of people that you could use your ability on.
- `!vote [mention|num]` -> Accuse a person during the accusation phase, either by mentioning him or using the number given by !party
- `!guilty` and `!innocent` -> Once someone has been put on trial, you can either vote guilty or innocent

Some of the more advanced commands:
- `pg.nomin [1|0]` -> Bypasses the minimum required players set by the game mode. Obviously not recommended, but whatever floats your boat.

## Getting in contact
If you have suggestions for roles that could make an interesting game, or any questions, you can reach me at amrojjeh@outlook.com
