package town.events;

import net.dv8tion.jda.api.JDA;
import town.DiscordGame;
import town.persons.Person;
import town.phases.Night;

public class MurderTownEvent implements TownEvent
{
	private Person murderer;
	private Person victim;
	private DiscordGame game;


	public MurderTownEvent(DiscordGame game, Person m, Person v)
	{
		this.game = game;
		murderer = m;
		victim = v;
	}

	public Person getMurderer()
	{
		return murderer;
	}

	public Person getVictim()
	{
		return victim;
	}

	@Override
	public DiscordGame getGame()
	{
		return game;
	}

	@Override
	public JDA getJDA()
	{
		return game.getJDA();
	}

	@Override
	public void standard(Person person)
	{
		if (person == getMurderer() && getGame().getCurrentPhase() instanceof Night)
			killVictim(person);
	}

	public void killVictim(Person person)
	{
		murderer.sendMessage("You killed " + getVictim().getRealName() + " (" + getVictim().getNickName() + ")");
		getVictim().die();
		System.out.println(getMurderer().getRealName() + " killed " + getVictim().getRealName());
	}

}
