package town.phases;

public class Initial extends Phase
{
	public Initial(PhaseManager pm)
	{
		super(pm);
	}

	@Override
	public void end()
	{
		getGame().sendMessageToTextChannel("system", "Game is starting").queue();
	}

	@Override
	public Phase getNextPhase(PhaseManager pm)
	{
		if (getGame().getPlayers().size() == getGame().getGameGuild().getMemberCount() - 1)
			return new Day(pm);
		return new Initial(pm);
	}

	@Override
	public int getDurationInSeconds()
	{
		return 10;
	}
}