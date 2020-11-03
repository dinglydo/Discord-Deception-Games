package town.mafia.roles;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import town.mafia.phases.Morning;
import town.mafia.phases.Night;
import town.persons.AttributeValue;
import town.persons.Attributes;
import town.persons.DiscordGamePerson;
import town.phases.Phase;
import town.roles.EmptyRoleData;
import town.roles.Faction;
import town.roles.Role;
import town.roles.RoleData;

/**
 * The Medium class represnts the role "Medium."
 * Medium is able to talk to the dead during the night.
 * This is done by giving access to the dead textchannel during the night, and revoking that channel during the day.
 * @author Amr Ojjeh
 *
 */
public class Medium implements Role
{
	public final String name = "Medium";
	public final Attributes attr = new Attributes(AttributeValue.NONE, AttributeValue.NONE);

	@Override
	public String getHelp()
	{
		return "You can talk to the dead during the night.";
	}

	@Override
	public void onPhaseChange(@Nonnull DiscordGamePerson user, @Nonnull Phase phase)
	{
		// The default method checks if user and phase are null
		Role.super.onPhaseChange(user, phase);
		if (phase instanceof Night && user.isAlive())
			user.getGame().setChannelVisibility(user, "the_afterlife", true, true).queue();
		else if (phase instanceof Morning && user.isAlive())
			user.getGame().getTextChannel("the_afterlife").getPermissionOverride(user.getMember()).delete().queue();
	}

	@Override
	@Nonnull
	public Attributes getAttributes()
	{
		return attr;
	}

	@Override
	@Nullable
	public List<DiscordGamePerson> getPossibleTargets(DiscordGamePerson user)
	{
		return null;
	}

	@Override
	@Nonnull
	public String ability(DiscordGamePerson user, List<DiscordGamePerson> list)
	{
		return getName() + " has no ability";
	}

	@Override
	@Nonnull
	public String getName()
	{
		return name;
	}

	@Override
	@Nonnull
	public Faction getFaction()
	{
		return Faction.TOWN;
	}

	@Override
	@Nonnull
	public RoleData getInitialRoleData()
	{
		return new EmptyRoleData();
	}

	@Override
	public int getPriority()
	{
		return 0;
	}
}
