package io.github.dinglydo.town.roles;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import io.github.dinglydo.town.discordgame.DiscordGame;
import io.github.dinglydo.town.mafia.phases.Morning;
import io.github.dinglydo.town.mafia.roles.TVMRole;
import io.github.dinglydo.town.persons.Attributes;
import io.github.dinglydo.town.persons.DiscordGamePerson;
import io.github.dinglydo.town.phases.Phase;

public interface Role
{
	DiscordGame getGame();

	TVMRole getRole();

	ArrayList<DiscordGamePerson> getPlayers();

	default void addPlayer(DiscordGamePerson person)
	{
		getPlayers().add(person);
		getFaction().getPlayers().add(person);
	}

	default void removePlayer(DiscordGamePerson person)
	{
		getPlayers().remove(person);
		getFaction().getPlayers().remove(person);
	}

	default int getPlayerAmount()
	{
		return getPlayers().size();
	}

	default Attributes getAttributes()
	{
		return getRole().getAttributes();
	}

	List<DiscordGamePerson> getPossibleTargets(DiscordGamePerson user);

	String ability(DiscordGamePerson user, List<DiscordGamePerson> list);

	default boolean hasWon(@Nonnull DiscordGamePerson user)
	{
		if (user == null) throw new NullPointerException("User cannot be an exception");
		return user.getGame().getFactionManager().hasTownFactionWon(getFaction());
	}

	default boolean canWin(@Nonnull DiscordGamePerson user)
	{
		if (user == null) throw new NullPointerException("User cannot be an exception");
		return getFaction().canWin();
	}

	default void win(@Nonnull DiscordGamePerson user)
	{
		if (user == null) throw new NullPointerException("User cannot be an exception");
		getFaction().win();
	}

	String getHelp();

	default String getName()
	{
		return getRole().getName();
	}

	Faction getFaction();

	RoleData getInitialRoleData();

	default int getPriority()
	{
		return getRole().getPriority();
	}

	/**
	 * Cancel the planned action.
	 * @return A string meant for the user to read.
	 */
	default String cancelAction(@Nonnull DiscordGamePerson user)
	{
		if (user == null) throw new NullPointerException("User cannot be an exception");
		if (user.getTownEvent() == null) return "There's no action to cancel";
		user.clearTownEvent();
		return "Action canceled";
	}

	/**
	 * This method should be called when the phase changes.
	 * @param phase The new phase.
	 */
	default void onPhaseChange(@Nonnull DiscordGamePerson user, @Nonnull Phase phase)
	{
		if (user == null) throw new NullPointerException("User cannot be null");
		if (phase == null) throw new NullPointerException("Phase cannot be null");
		if (phase instanceof Morning)
		{
			user.clearTownEvent();
			user.clearTemporaryAttributes();
		}
	}
}
