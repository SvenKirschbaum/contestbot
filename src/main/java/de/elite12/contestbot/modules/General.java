package de.elite12.contestbot.modules;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

@Autoload
@EventTypes({ Events.MESSAGE, Events.WHISPER })
public class General implements EventObserver {// TODO: Reaktion bei Spende

	private static Logger logger = Logger.getLogger(General.class);

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
			}
		}
	}
	
	private boolean ispermitted(Message m) {
		return m.getTags().containsKey("mod") ? m.getTags().get("mod").equals("1") : false;
	}
}