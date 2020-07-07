package town.phases;

import town.util.RestHelper;

public class Night extends Phase
{
	public Night(PhaseManager pm)
	{
		super(pm);
	}

	@Override
	public void start()
	{
		getGame().setChannelVisibility("player", "daytime_discussion", true, false).queue();

		RestHelper.queueAll(getGame().toggleVC("Daytime", false), getGame().muteAllInRole("defendant", false));

		getGame().getPlayers().forEach(person -> person.sendMessage("Night " + getGame().getDayNum() + " started"));
		phaseManager.setWarningToAll(5);
	}

	@Override
	public void end()
	{
		getGame().dispatchEvents();
		getGame().nextDayStarted();
	}

	@Override
	public Phase getNextPhase()
	{
		return new Morning(phaseManager);
	}

	@Override
	public int getDurationInSeconds()
	{
		return 40;
	}
}
