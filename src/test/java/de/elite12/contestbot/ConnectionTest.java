/*******************************************************************************
 * Copyright (C) 2017 Sven Kirschbaum
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.elite12.contestbot;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.experimental.runners.Enclosed;

@RunWith(Enclosed.class)
public class ConnectionTest {

	private static ConnectionStub connection;
	
	@After
	public void setUpAfterTest() throws Exception {
		MessageParser.queue.clear();
		ConnectionTest.connection.output.clear();
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		new ContestBotStub();
		ConnectionTest.connection = new ConnectionStub();
	}

	public static class SingleTests {
		@Test
		public void testOnOpen() {
			ConnectionTest.connection.onOpen(null);
			assertEquals("Password send incorrect", "PASS oauth:h4sMKbBUZQ8cQATzcjDzE7dxesd2v5mT", connection.output.remove());
			assertEquals("Nick send incorrect", "NICK gYbrCrkCqhsw5pwL", connection.output.remove());
			assertNull(connection.output.poll());
		}
	}

	@RunWith(Parameterized.class)
	public static class ParameterTests {
		@Parameters
		public static Collection<String> data() {
			return Arrays.asList(new String[] {
				"2005 LUL",
				"12 btw haHAA",
				"tyty",
				"and their phones",
				"LUL",
				"LUL haHAA",
				"monkaS",
				"fraggit",
				"@summit1g stop texting chimpmunk",
				"monkaS",
				"RieseGod smells",
				"haHAA 12 BTW",
				"cjip",
				"sumW",
				"Thanks @Zyvexal",
				"we demand a sumPhone emote",
				"np",
				"HeyGuys",
				"shroud playing like a pussy",
				"2",
				"give it up already @riesegod",
				"@JLD312 Hey look.. its the scruncher LUL",
				"Check out summit's latest YouTube video: Stream Highlights #146 youtube.com/watch?v=QQquNhQX1AQ",
				"whos chimpmunk",
				"sumStepdad",
				"Chats the side hoe feelsbadman",
				"anthony ongphan would have 20 kills by now",
				"@summit1g you’ve been getting off early , skills are lackin, on the phone a lot whips the grill sumThump don’t forget us SAMAMCHU",
				"sumPhone!!!!",
				"@ToooStrongKC who are you? LUL",
				"I MEAN CHIP",
				"sory for that mods im a (idiot)",
				"Why is chat picking on my boy RieseGod? sumE",
				"RieseGod calling people scrunchers haHAA",
				"lol right @riesegod",
				"ACTION sumBuhblam ✧ RESUB HYPE! (ﾉ◕ヮ◕)ﾉ sumBuhblam Duogie stayed on the 1G Squad!! for 3 months! sum1g sumLove sum1g",
				"someone is going to clip that and send it to doc",
				"Naelphua cause riese is a giant nerd",
				"@summit1g have you ever been to Canada? If so where? sumGold",
				"U didn't pick up the level 2 helmet summit",
				"@iiCanMakeYouSmile im the chipmunk",
				"sumStache ☎️☎️",
				"Hmmm JLD312 fitfgHmm",
				"grimmz would have already won the game by now with 40+ killsa dn a 360 no scope for the win",
				"@FadedChipmunkx2 sumW",
				"@Naelphua Stay away from @JLD312 he scrunches WutFace",
				"@RieseGod We Love RieseGod sumLUL",
				"any1 got the 1k reaction ?",
				"WutFace",
				"sumAyo",
				"RieseGod stands",
				"sup guys",
				"@s3thFPS KappaPride",
				"WutFace",
				"hillbilly DansGame",
				"cmonBruh",
				"if summit doesn't text 1 time while playing with chip its confirmed sumSwag",
				"I've never been on a plane. Lol",
				"i swear he says that about 5 times a day",
				"Baby wipes and sitting. Only way to go chat sumE",
				"ACTION sum1g ✧ NEW SUB!! (ﾉ◕ヮ◕)ﾉ sumBuhblam durkaderka welcome to the 1G Squad!! sum1g sumLove sum1g",
				"@JLD312 lies from a scruncher WutFace",
				"@summit1g you better go traveling once you retire at 35",
				"RieseGod psl",
				"@summit1g do you ever plan on leaving? xd or is it a special occasion kinda thing?",
				"pls",
				"Come to Canada",
				"summit1g any plans coming to dreamhack in sweden maybe one day ?",
				"You haven't lived until you've traveled abroad, come on man.",
				"sumE ☎️☎️",
				"How many wins?",
				"Come to Norway",
				"Been to every continent but antartica FeelsGoodMan",
				"2",
				"@JLD312 scrunchers trying to spread lies WutFace",
				"Incoming donation of 1 015$ ?",
				"@Zyvexal why would he retire when he makes bank doing what he loves?",
				"plebs that want to be freed LUL",
				"^^^",
				"HotBoi",
				"HotBoi",
				"M16 with 4x Kreygasm",
				"your turn @brillantjs",
				"@jbyrd22221 he's gonna have enough money and it's always good to expand ur horizons",
				"@summit1g Have to get out and explore the world bud. So amny amazing things to see",
				"HotBoi",
				"monkaGIGA",
				"@summit1g are you retiring at 65?",
				"monkaS",
				"plebs dont have $5 LUL",
				"monkaS",
				":O",
				"I'm in the same boat as @summit1g i've never left Canada! FeelsBadMan",
				"RUST",
				"this is a pleb free stream",
				"Play some what?",
				"RUUSSSTTTTT",
				"@cermi3 I was already outdonated it's someone else's turn for the 1 015$ man",
				"YEASSSSSS",
				"NOT IN CANADA sumW",
				"LUL",
				"LUL",
				"RUST???",
				"or amazon prime LUL",
				"rust would be dank",
				"Follow summit on Twitter @ www.twitter.com/summit1g (Last Tweet: !tweet )",
				"sumW sumE",
				"PogChamp",
				"Rust legacy .. HNNG",
				"\"young\" Kappa",
				"I care about it when I care about it lol",
				"It's not about sightseeing, it's about experiencing other cultures.",
				"@summit1g you can still play old rust",
				"!monster",
				"Legccyyyyyyy",
				"STREAM POWERED BY MONSTER ENERGY™ THE FUEL FOR SUCCESS",
				"young sumW",
				"where's rustintimberlake been",
				"@summit1g old Rust is gone now though, devs removed it.",
				"The only interesting thing in Canada is the border to come back into the US Kappa",
				"Old Rust was fucking OP as fuck foreal. Stopped playing when devs made new rust",
				"@Zyvexal He can travel without retiring",
				"old rust FeelsBadMan",
				"@ToxicKaos56 you can still play legacy i play it alot",
				"sumE",
				"@mememasterbob or free health care",
				"@summit1g if you go to legion rust tthey show you how to play rust legacy its nutty",
				"why people care so much about your vacations? @summit1g",
				"lul yong sumsum now hes 80years old?",
				"oh shit Matty with the top d today PogChamp",
				"holo sight Kreygasm",
				"Play rust PogChamp",
				"fail link",
				"Link cut off FeelsBadMan",
				"Hockey is fun as hell to watch in Canada",
				"BabyRage baby wipes BabyRage",
				"I can't imagine summit being a grandpa when he's 60 saying he never left his house and played games for a living. What an icon. sumGasm",
				"lmao the link LUL",
				"nice link LUL",
				"Summit being a granpa LUL",
				"@summit1g Facepunch is adding a fuckin AI tank that protects the new rocket launch site monument.",
				"@s3thFPS lol",
				"All the cs:go pro scene shuffles drama sumGasm",
				"Guys not being funny but Gamers die by the age of 55",
				"Eh it's not that important. @RieseGod",
				"ACTION sumLove ✧ RESUB HYPE! (ﾉ◕ヮ◕)ﾉ sumBuhblam 720noscopedive stayed on the 1G Squad!! for 3 months! sum1g sumLove sum1g",
				"still cooler than you though sumLUL @s3thFPS",
				"@summit1g hey are you a broncos fan?"
			});
		}
		
		@Parameter(0)
		public String input;
		
		@Test
		public void testSendChatMessage() {
			ConnectionTest.connection.sendChatMessage(input);
			assertEquals("Message command format error", String.format("PRIVMSG #blubabc123 :%s", input), connection.output.remove());
			assertNull(MessageParser.queue.poll());
		}
	
		@Test
		public void testSendPrivatMessage() {
			ConnectionTest.connection.sendPrivatMessage("fallobst22", input);
			assertEquals("Message command format error", String.format("PRIVMSG #blubabc123 :/w fallobst22 %s", input), connection.output.remove());
			assertNull(MessageParser.queue.poll());
		}
	}
	
	private static final class ConnectionStub extends Connection{
		
		Queue<String> output;

		public ConnectionStub() throws URISyntaxException {
			super();
			output = new ArrayDeque<>();
		}
		
		@Override
		public void send(String text) throws NotYetConnectedException {
			output.add(text);
		}
	}
	
	private static final class ContestBotStub extends ContestBot{
		public ContestBotStub() {
			super(true);
			ContestBot.instance = this;
		}
		
		@Override
		public String getConfig(String key) {
			switch(key) {
				case "ircserver": {
					return "wss://irc-ws.chat.twitch.tv";
				}
				case "oauth": {
					return "oauth:h4sMKbBUZQ8cQATzcjDzE7dxesd2v5mT";				
				}
				case "login": {
					return "gYbrCrkCqhsw5pwL";
				}
				case "channelname": {
					return "blubabc123";
				}
				default: {
					return "";
				}
			}
		}
	}
}
