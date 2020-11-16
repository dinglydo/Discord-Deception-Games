package io.github.dinglydo.town.discordgame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.dinglydo.town.DiscordGameConfig;
import io.github.dinglydo.town.MainListener;
import io.github.dinglydo.town.events.TownEvent;
import io.github.dinglydo.town.mafia.phases.End;
import io.github.dinglydo.town.mafia.roles.TVMRole;
import io.github.dinglydo.town.party.Party;
import io.github.dinglydo.town.persons.DiscordGamePerson;
import io.github.dinglydo.town.persons.assigner.Assigner;
import io.github.dinglydo.town.phases.Phase;
import io.github.dinglydo.town.phases.PhaseManager;
import io.github.dinglydo.town.roles.Faction;
import io.github.dinglydo.town.roles.Role;
import io.github.dinglydo.town.util.RestHelper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction;
import net.dv8tion.jda.api.requests.restaction.GuildAction.RoleData;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;

//// This represents an ongoing deception game. It's instantiated with pg.startParty
public class DiscordGame
{
	private final MainListener ml;
	private final DiscordGameConfig config;

	// Important channels (Name : id)
	private HashMap<String, Long> channels = new HashMap<>();
	private DiscordRoles discordRoles = new DiscordRoles(this);
	private HashSet<Faction> wonTownRoles = new HashSet<Faction>();
	private PriorityQueue<TownEvent> events = new PriorityQueue<>();
	private PhaseManager phaseManager = new PhaseManager();

	private LinkedList<DiscordGamePerson> savedForMorning = new LinkedList<>();
	private ArrayList<DiscordGamePerson> players = new ArrayList<>();
	private ArrayList<Role> roles = new ArrayList<>();
	private FactionManager factionManager = new FactionManager(this);
	private Assigner assignerUsed;

	private int dayNum = 1;
	private boolean ended = false;

	// For listener
	DiscordGameListener listener = new DiscordGameListener(this);
	boolean serverCreated = false;
	boolean registeredListener = false;
	long identifier = 0;
	long gameGuildId;

	private DiscordGame(MainListener ml, DiscordGameConfig config)
	{
		this.ml = ml;
		this.config = config;
	}

	public JDA getJDA()
	{
		return ml.getJDA();
	}

	public String getPrefix()
	{
		return "!";
	}

	public DiscordGameConfig getConfig()
	{
		return config;
	}

	public ArrayList<DiscordGamePerson> getPlayersCache()
	{
		return players;
	}

	public ArrayList<Role> getRoles()
	{
		return roles;
	}

	// TODO: Check for duplicates
	public void addRole(Role role)
	{
		if (getRole(role.getRole()) == null)
			getRoles().add(role);
		else throw new IllegalArgumentException("");
	}

	@Nullable
	public Role getRole(TVMRole role)
	{
		for (Role r : getRoles())
		{
			if (r.getRole() == role)
				return r;
		}
		return null;
	}

	public FactionManager getFactionManager()
	{
		return factionManager;
	}

	public Assigner getAssignerUsed()
	{
		return assignerUsed;
	}

	public void registerAsListener(boolean register)
	{
		if (register && !registeredListener)
		{
			getJDA().addEventListener(listener);
			registeredListener = true;
		}
		else if (!register && registeredListener)
		{
			getJDA().removeEventListener(listener);
			registeredListener = false;
		}
	}

	public boolean isRegisteredListener()
	{
		return registeredListener;
	}

	public static DiscordGame createServer(Party party, long identifier)
	{
		DiscordGame game = new DiscordGame(party.getMainListener(), party.getConfig());
		game.identifier = identifier;
		game.registerAsListener(true);
		game.assignerUsed = game.getConfig().getGameMode().build(party, game, game.getConfig().isRandom());
		game.createServer();
		return game;
	}

	private void createServer()
	{
		GuildAction ga = getJDA().createGuild(config.getGameMode().getName());
		createNewChannels(ga);
		ga.newRole().setName("" + identifier);
		ga.queue();
	}

	private void createNewChannels(GuildAction g)
	{
		// this channel used for general game updates
		for (Role role : getRoles())
			g.newRole().setName(role.getName()).setPermissionsRaw(0l);

		g.newRole().setName("Bot").addPermissions(Permission.ADMINISTRATOR).setColor(Color.YELLOW)
		.setHoisted(true);

		g.newRole().setName("Player").setColor(Color.CYAN)
		.setPermissionsRaw(QP.readPermissions() | QP.writePermissions() | QP.speakPermissions())
		.setHoisted(true);

		RoleData deadPlayerRoleData = g.newRole().setName("Dead").setColor(Color.GRAY)
				.setPermissionsRaw(QP.readPermissions())
				.setHoisted(true);

		RoleData defendantRoleData = g.newRole().setName("Defendant").setColor(Color.GREEN)
				.setPermissionsRaw(QP.speakPermissions() | QP.writePermissions() | QP.readPermissions())
				.setHoisted(true);

		// players discussing during the day
		g.newChannel(ChannelType.TEXT, "daytime_discussion")
		.setPosition(0)
		.addPermissionOverride(g.getPublicRole(), QP.readPermissions(), QP.writePermissions())
		.addPermissionOverride(defendantRoleData, QP.readPermissions() | QP.writePermissions(), 0);

		for (DiscordGamePerson p : getPlayersCache())
		{
			g.newChannel(ChannelType.TEXT, "private")
			.setPosition(1)
			.addPermissionOverride(g.getPublicRole(), 0, QP.readPermissions() | QP.writePermissions())
			.setTopic(p.getRealName()); // This will be used as an identifier
		}

		//for dead players
		g.newChannel(ChannelType.TEXT, "the_afterlife")
		.setPosition(2)
		.addPermissionOverride(g.getPublicRole(), 0, QP.readPermissions() | QP.writePermissions())
		.addPermissionOverride(deadPlayerRoleData, QP.readPermissions() | QP.writePermissions(), 0);
	}

	public void endGame()
	{
		ended = true;
		phaseManager.end();
		RestHelper.queueAll
		(
				getDiscordRole("player").muteAllInRole(false),
				setChannelVisibility("dead", "daytime_discussion", true, true),
				setChannelVisibility("player", "daytime_discussion", true, true),
				setChannelVisibility("player", "the_afterlife", true, true),
				sendMessageToTextChannel("daytime_discussion",
				"The game has ended! You can either `!delete` the server or `!transfer`" +
				" the server. In 60 seconds if no choice is made, the server will delete itself." +
				" (To transfer, the party leader must be in the server)")
		);
		openPrivateChannels();
		phaseManager.start(this, new End(this, phaseManager));
	}

	public boolean hasEnded()
	{
		return ended;
	}

	public boolean wasServerCreated()
	{
		return serverCreated;
	}

	public void deleteServer()
	{
		phaseManager.end();
		registerAsListener(false);
		getGuild().delete().queue();
	}

	public void transfer(@Nonnull Member member)
	{
		phaseManager.end();
		registerAsListener(false);
		getGuild().transferOwnership(member).reason("The game has ended").queue();
	}

	@Nullable
	public DiscordGamePerson getPerson(Member member)
	{
		return getPerson(member.getIdLong());
	}

	@Nullable
	private DiscordGamePerson getPerson(long id)
	{
		for (DiscordGamePerson person : getPlayersCache())
			if (person.getID() == id)
				return person;
		return null;
	}

	public int getReferenceFromPerson(@Nonnull DiscordGamePerson person)
	{
		int ref = getPlayersCache().indexOf(person) + 1;
		if (ref <= 0)
			throw new IllegalArgumentException("Person does not exist");
		return ref;
	}

	public DiscordGamePerson getPersonFromReference(int ref)
	{
		return getPlayersCache().get(ref - 1);
	}

	public ArrayList<DiscordGamePerson> getAlivePlayers()
	{
		ArrayList<DiscordGamePerson> alive = new ArrayList<>();
		getPlayersCache().stream().filter(p -> p.isAlive()).forEach(p -> alive.add(p));
		return alive;
	}

	public TextChannel getTextChannel(String channelName)
	{
		if (!serverCreated) throw new IllegalStateException("Server not created yet");
		Long channelID = channels.get(channelName);
		return getTextChannel(channelID);
	}

	public TextChannel getTextChannel(long channelID)
	{
		if (!serverCreated) throw new IllegalStateException("Server not created yet");
		return getGuild().getTextChannelById(channelID);
	}

	public VoiceChannel getVoiceChannel(String channelName)
	{
		if (!serverCreated) throw new IllegalStateException("Server not created yet");
		return getGuild().getVoiceChannelsByName(channelName, false).get(0);
	}

	public void addEvent(TownEvent event)
	{
		events.add(event);
	}

	public void removeEvent(TownEvent event)
	{
		events.remove(event);
	}

	public void dispatchEvents()
	{
		if (events.size() == 0) return;
		TownEvent event = events.remove();
		for (DiscordGamePerson person : getPlayersCache())
			person.onEvent(event);

		event.postDispatch();
		dispatchEvents();
	}

	public Phase getCurrentPhase()
	{
		return phaseManager.getCurrentPhase();
	}

	public User getUser(DiscordGamePerson person)
	{
		return getJDA().getUserById(person.getID());
	}

	public long getGuildId()
	{
		return gameGuildId;
	}

	public Guild getGuild()
	{
		if (!serverCreated) throw new IllegalStateException("Server not created yet");
		return getJDA().getGuildById(getGuildId());
	}

	/**
	 * Return discord role via name
	 * @param roleName Role name
	 * @return the DiscordRole corresponding to the name
	 */
	@Nullable
	public DiscordRole getDiscordRole(String roleName)
	{
		for (DiscordRole role : discordRoles)
		{
			if (role.getName().equalsIgnoreCase(roleName))
			{
				return role;
			}
		}
		return null;
	}

	public MessageAction sendMessageToTextChannel(String channelName, String msg)
	{
		return getTextChannel(channelName).sendMessage(msg);
	}

	public MessageAction sendMessageToTextChannel(Long channelID, String msg)
	{
		return getTextChannel(channelID).sendMessage(msg);
	}

	public MessageAction sendMessageToTextChannel(String channelName, MessageEmbed embed)
	{
		return getTextChannel(channelName).sendMessage(embed);
	}

	public MessageAction sendMessageToTextChannel(Long channelID, MessageEmbed embed)
	{
		return getTextChannel(channelID).sendMessage(embed);
	}

	public RestAction<Message> getMessage(String channelName, long messageID)
	{
		return getTextChannel(channelName).retrieveMessageById(messageID);
	}

	public void startGame()
	{
		config.getGameMode().start(this, phaseManager);
	}


	public RestAction<?> toggleVC(String channelName, boolean show)
	{
		if (!show)
			return getVoiceChannel(channelName).delete();
		else
			return getGuild().createVoiceChannel("Daytime");
	}

	public PermissionOverrideAction setChannelVisibility(String roleName, String channelName, boolean read, boolean write)
	{
		return setChannelVisibility(getDiscordRole(roleName).getRole(), channelName, read, write);
	}

	/**
	 * Open private channels to all players
	 */
	public void openPrivateChannels()
	{
		getPlayersCache().forEach(p -> setChannelVisibility(getGuild().getPublicRole(), p.getPrivateChannel(), true, false).queue());
	}

	public PermissionOverrideAction setChannelVisibility(DiscordGamePerson p, String channelName, boolean read, boolean write)
	{
		if (p.isDisconnected()) return null;
		Member member = p.getMember();
		if (member == null) throw new IllegalArgumentException("Invalid person.");
		return setChannelVisibility(member, channelName, read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, String channelName, boolean read, boolean write)
	{
		return setChannelVisibility(holder, getTextChannel(channelName), read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, long channelId, boolean read, boolean write)
	{
		return setChannelVisibility(holder, getTextChannel(channelId), read, write);
	}

	private PermissionOverrideAction setChannelVisibility(IPermissionHolder holder, TextChannel channel, boolean read, boolean write)
	{
		if (channel == null) throw new IllegalArgumentException("Channel name doesn't exist");
		PermissionOverrideAction action = null;
		if (channel.getType().equals(ChannelType.TEXT))
		{
			if (read && !write)
				action = channel.putPermissionOverride(holder).reset().setPermissions(QP.readPermissions(), QP.writePermissions());
			else if (read && write)
				action = channel.putPermissionOverride(holder).reset().setAllow(QP.readPermissions() | QP.writePermissions());
			else
				action = channel.putPermissionOverride(holder).reset().setDeny(QP.readPermissions() | QP.writePermissions());
		}

		return action;
	}

	public void winTownFaction(Faction faction)
	{
		wonTownRoles.add(faction);
	}

	public boolean hasTownFactionWon(Faction faction)
	{
		return wonTownRoles.contains(faction);
	}

	public void saveForMorning(DiscordGamePerson p)
	{
		savedForMorning.add(p);
	}

	public DiscordGamePerson getDeathForMorning()
	{
		if (savedForMorning.isEmpty())
			return null;
		return savedForMorning.pop();
	}

	public DiscordGamePerson peekDeathForMorning()
	{
		return savedForMorning.peek();
	}

	public int getDayNum()
	{
		return dayNum;
	}

	public void startNextDay()
	{
		dayNum++;
	}

	public void assignRoles(Guild guild)
	{
		guild.addRoleToMember(getJDA().getSelfUser().getIdLong(), guild.getRolesByName("Bot", false).get(0)).queue();

		List<net.dv8tion.jda.api.entities.Role> guildRoles = guild.getRoles();
		for (int x = 0; x < getRoles().size(); ++x)
		{
			net.dv8tion.jda.api.entities.Role townRole = guildRoles.get(guildRoles.size() - x - 2);
			discordRoles.add(townRole.getName(), townRole.getIdLong());
		}

		DiscordRole playerRole = new DiscordRole(this, "player", guild.getRolesByName("Player", false).get(0).getIdLong());
		discordRoles.add(playerRole);

		getPlayersCache().forEach(person -> person.addDiscordRole(playerRole));

		discordRoles.add("dead", guild.getRolesByName("Dead", false).get(0).getIdLong());
		discordRoles.add("defendant", guild.getRolesByName("Defendant", false).get(0).getIdLong());

		for (DiscordGamePerson p : getPlayersCache())
		{
			p.sendMessage("Your role is " + p.getRole().getName());
			p.sendMessage(p.getRole().getHelp());
		}

	}

	public void assignChannel(GuildChannel channel)
	{
		if (!channel.getName().contentEquals("private")) channels.put(channel.getName(), channel.getIdLong());
		else
			for (DiscordGamePerson p : getPlayersCache())
			{
				TextChannel textChannel = (TextChannel)channel;
				String topic = textChannel.getTopic();
				if (topic != null && topic.contains(p.getRealName()))
				{
					p.setPrivateChannel(textChannel.getIdLong());
					return;
				}
			}
	}

	public void sendInviteToPlayers(Guild guild)
	{
		guild.getChannels().get(0).createInvite().queue((invite) -> getPlayersCache().forEach((person) -> person.sendDM(invite.getUrl())));
	}
}
