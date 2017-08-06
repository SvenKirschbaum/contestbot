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
package de.elite12.contestbot.modules;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;

import de.elite12.contestbot.ContestBot;
import de.elite12.contestbot.LockHelper;
import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Message;
import de.elite12.contestbot.SQLite;

@Autoload
@EventTypes({ Events.MESSAGE, Events.WHISPER })
public class General implements EventObserver {

	private static Logger logger = Logger.getLogger(General.class);
	private static Pattern pattern = Pattern.compile("^(\\w+) haut €(\\d+)\\.(\\d{2}) raus, DerInder dankt! derindWTF$");

	private String oauthkey;
	private String channelid;
	private Client client;

	public General() {
		this.oauthkey = ContestBot.getInstance().getConfig("oauth").split(":")[1];
		
		this.client = ClientBuilder.newClient().register(new Feature() {
			@Override
			public boolean configure(FeatureContext context) {
				context.register(new ClientRequestFilter() {
					@Override
					public void filter(ClientRequestContext requestContext) throws IOException {
						requestContext.getHeaders().add("Authorization", String.format("OAuth %s", oauthkey));
					}
				});
				return true;
			}
		});
		WebTarget target = client.target("https://api.twitch.tv/kraken/users").queryParam("login",
				ContestBot.getInstance().getConfig("channelname"));
		this.channelid = target.request("application/vnd.twitchtv.v5+json").get(JsonObject.class).getJsonArray("users")
				.getJsonObject(0).getString("_id");
		logger.debug(String.format("Loaded Channelid %s", this.channelid));
	}

	@Override
	public void onEvent(Events type, Event e) {
		boolean whisper = (type == Events.WHISPER);
		Message m = (Message) e;

		if (m.getMessage().charAt(0) == '!') {
			String[] split = m.getMessage().split(" ", 2);
			split[0] = split[0].toLowerCase();
			switch (split[0]) {
				case "!discord": {
					if(LockHelper.checkAccess("!discord", ispermitted(m), whisper))
						ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"Discord: https://discord.gg/YmvhsxX");
					break;
				}
				case "!twitter": {
					if(LockHelper.checkAccess("!twitter", ispermitted(m), whisper))
						ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"Twitter: https://twitter.com/derinderr");
					break;
				}
				case "!ts": {
					if(LockHelper.checkAccess("!ts", ispermitted(m), whisper))
						ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"TS ist kacke, benutzt lieber !discord!");
					break;
				}
				case "!hardware": {
					if(LockHelper.checkAccess("!hardware", ispermitted(m), whisper))
						ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"GTX 770 2GB");
					ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"I5 4690 @ 3,7Ghz");
					ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
							"8GB DDR3 RAM");
					break;
				}
				case "!uptime": {
					if(LockHelper.checkAccess("!uptime", ispermitted(m), whisper)) {
						JsonValue r = this.client.target("https://api.twitch.tv/kraken/streams/").path(this.channelid)
								.request("application/vnd.twitchtv.v5+json").get(JsonObject.class).get("stream");
						if (r.getValueType() == ValueType.OBJECT) {
							JsonObject stream = (JsonObject) r;
							Duration d = Duration.between(ZonedDateTime.parse(stream.getString("created_at"),
									DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("Z"))), ZonedDateTime.now());
							if ((d.getSeconds() / 3600) >= 1) {
								ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
										String.format("Der Stream ist seit %d Stunden und %d Minuten online",
												d.getSeconds() / 3600, (d.getSeconds() / 60) % 60));
							} else {
								ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
										String.format("Der Stream ist seit %d Minuten online", d.getSeconds() / 60));
							}
						} else {
							ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
									"Der Stream ist nicht live");
						}
					}
					break;
				}
				case "!ripdevil": {
					if(!whisper && LockHelper.checkAccess("!ripdevil", ispermitted(m), false)) {
						//I know this is a bad hack, but ....
						try {
							SQLite.getInstance().changePoints("!ripdevil", 1);
							ContestBot.getInstance().getConnection().sendChatMessage(
									String.format("RIP Devil Count: %d", SQLite.getInstance().getPoints("!ripdevil")));
						} catch (SQLException e1) {
							logger.error("Error in !ripdevil",e1);
						}	
					}
					break;
				}
			}
		}
		if(m.getUsername().equalsIgnoreCase(ContestBot.getInstance().getConfig("channelname"))) {
			Matcher matcher = pattern.matcher(m.getMessage());
			if(matcher.matches()) {
				String user = matcher.group(1);
				Integer euro = Integer.parseInt(matcher.group(2));
				Integer cent = Integer.parseInt(matcher.group(3));
				
				ContestBot.getInstance().getConnection().sendChatMessage(String.format("%s <3", user));
				logger.info(String.format("%s spendet %d.%d€", user, euro, cent));
			}
		}
	}
	
	private boolean ispermitted(Message m) {
		return m.getTags().containsKey("mod") ? m.getTags().get("mod").equals("1") : false;
	}
}
