package town.games.parser;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import town.TownRole;
import town.games.GameMode;

public class GameParser
{
	public static GameMode parseGeneralGame(String str)
	{
		if (str.isBlank())
			throw new IllegalArgumentException("Could not parse string because it was empty.");
		String name = "Custom Game";
		String description = "";

		int currentLine = 0;
		String[] lines = str.split("\n");

		if (isGameName(lines[currentLine]))
		{
			String[] first = lines[currentLine++].split(":", 2);
			name = first[0];
			if (first.length == 2)
				description = first[1];
		}

		GameMode game = new GameMode(name, description, false);
		while (currentLine != lines.length)
		{
			Rule rule = parseRule(lines[currentLine++]);
			if (rule.totalPlayers != calculateImplicitTotalPlayers(rule.roles))
				throw new IllegalArgumentException("The total number of players does not equal the max number of players pass in the rule: " + str);
			game.addRule(rule);
		}
		return game;
	}

	// A game name would be the name that the guild server would use.
	// As long as the name is 2 characters or more, and it doesn't start with a digit, it's a valid name
	public static boolean isGameName(String str)
	{
		if (str.length() < 2) return false;
		if (str.isBlank()) return false;
		if (Character.isDigit(str.charAt(0))) return false;
		return true;
	}

	public static Rule parseRule(String str)
	{
		Pattern roleTuples = Pattern.compile("\\(.+?,\\s*\\d+\\+?\\s*\\)");

		Matcher matcher = roleTuples.matcher(str);
		Rule rule = new Rule(getExplicitTotalPlayers(str));
		while (matcher.find())
			rule.addRole(getRoleFromTuple(str.substring(matcher.start(), matcher.end())));

		return rule;
	}

	public static Role getRoleFromTuple(String tuple)
	{
		Pattern roleAndNum = Pattern.compile("\\((.+?),\\s*(\\d+)(\\+?)");
		Matcher match = roleAndNum.matcher(tuple);
		if (!match.find())
			throw new IllegalArgumentException("Could not parse: " + tuple);
		TownRole role = TownRole.getRoleFromName(match.group(1));
		return new Role(role, Integer.parseInt(match.group(2)), match.group(3).equals("+"));
	}

	public static int calculateImplicitTotalPlayers(ArrayList<Role> singleRoles)
	{
		int sum = 0;
		for (Role sr : singleRoles)
			sum += sr.max;
		return sum;
	}

	public static int getExplicitTotalPlayers(String str)
	{
		Pattern totalNumberOfPlayers = Pattern.compile("(\\d+)");
		Matcher matcher = totalNumberOfPlayers.matcher(str);
		if (!matcher.find())
			throw new IllegalArgumentException("No total number of players passed in: " + str);
		return Integer.parseInt(matcher.group(1));
	}
}
