package io.github.dinglydo.town.party;

import java.util.ArrayList;

import javax.annotation.Nullable;

import io.github.dinglydo.town.DiscordGameConfig;
import io.github.dinglydo.town.MainListener;
import io.github.dinglydo.town.commands.PartyCommands;
import io.github.dinglydo.town.persons.LobbyPerson;
import io.github.dinglydo.town.persons.assigner.Assigner;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Party extends ListenerAdapter
{
	private final MainListener ml;
	private final PartyCommands commands;
	private final long channelId;

	DiscordGameConfig config;
	private String prefix;
	private ArrayList<LobbyPerson> persons = new ArrayList<>();
	private boolean registeredListener;

	private Party(MainListener ml, long channelId, long partyLeaderId)
	{
		this.ml = ml;
		this.channelId = channelId;
		// TODO: Join the party leader and make him leader since no one's in the list
		config = new DiscordGameConfig();
		config.setGameMode("1");
		prefix = "pg.";
		registeredListener = false;
		commands = new PartyCommands();
	}

	public JDA getJDA()
	{
		return ml.getJDA();
	}

	public MainListener getMainListener()
	{
		return ml;
	}

	public static Party createParty(MainListener ml, TextChannel tc, Member member)
	{
		Party gp = new Party(ml, tc.getIdLong(), member.getIdLong());
		gp.registerAsListener(true);
		return gp;
	}

	public int getPlayerSize()
	{
		return getPlayersCache().size();
	}

	public ArrayList<LobbyPerson> getPlayersCache()
	{
		return persons;
	}

	public ArrayList<LobbyPerson> getPlayers()
	{
		return new ArrayList<>(persons);
	}

	public LobbyPerson getGameLeader() throws PartyIsEmptyException
	{
		if (getPlayersCache().isEmpty()) throw new PartyIsEmptyException(this);
		return getPlayersCache().get(0);
	}

	@Nullable
	public LobbyPerson getPerson(Member member)
	{
		return getPerson(member.getIdLong());
	}

	@Nullable
	public LobbyPerson getPerson(long id)
	{
		for (LobbyPerson person : getPlayersCache())
			if (person.getID() == id)
				return person;
		return null;
	}

	public boolean hasPersonJoined(LobbyPerson person)
	{
		return hasPersonJoined(person.getID());
	}

	public boolean hasPersonJoined(Member member)
	{
		return hasPersonJoined(member.getIdLong());
	}

	public boolean hasPersonJoined(long id)
	{
		return getPerson(id) != null;
	}

	public String getPrefix()
	{
		return prefix;
	}

	public DiscordGameConfig getConfig()
	{
		return config;
	}

	public long getChannelId()
	{
		return channelId;
	}

	public void registerAsListener(boolean register)
	{
		if (register && !registeredListener)
		{
			getJDA().addEventListener(this);
			registeredListener = true;
		}
		else if (!register && registeredListener)
		{
			getJDA().removeEventListener(this);
			registeredListener = false;
		}
	}

	public boolean isRegisteredListener()
	{
		return registeredListener;
	}

	public User getUser(LobbyPerson person)
	{
		return getJDA().getUserById(person.getID());
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent message)
	{
		if (message.getChannel().getIdLong() == channelId)
			processMessage(message.getMessage());
	}

	public boolean processMessage(Message message)
	{
		return processMessage(getPrefix(), message);
	}

	public boolean processMessage(String prefix, Message message)
	{
		return commands.executeCommand(this, prefix, message);
	}

	public boolean isPartyFull()
	{
		int size = getPlayerSize();
		Assigner assigner = getConfig().getGameMode().getClosestAssigner(size);
		return !assigner.hasDefault() && !getConfig().isRandom() && assigner.getMinimumPlayers() < size + 1;
	}

	public boolean isPartyEmpty()
	{
		return getPlayerSize() == 0;
	}

	public void joinGame(Member member) throws PartyIsFullException
	{
		joinGame(new LobbyPerson(this, member.getIdLong()));
	}

	public void joinGame(LobbyPerson person) throws PartyIsFullException
	{
		// Has person already joined?
		if (hasPersonJoined(person)) return;

		// Throw an exception if the lobby is already full
		if (isPartyFull()) throw new PartyIsFullException(this, person);

		// Add the person if everything succeeds
		getPlayersCache().add(person);
	}

	public void leaveGame(Member member) throws PartyIsEmptyException
	{
		leaveGame(getPerson(member));
	}

	public void leaveGame(LobbyPerson person) throws PartyIsEmptyException
	{
		if (isPartyEmpty()) throw new PartyIsEmptyException(this);
		getPlayersCache().remove(person);
	}

	public void endParty()
	{
		registerAsListener(false);
		getMainListener().endParty(this);
	}
}
