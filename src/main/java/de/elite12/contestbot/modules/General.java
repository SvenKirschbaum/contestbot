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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.log4j.Logger;

import de.elite12.contestbot.AuthProvider;
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
@EventTypes({ Events.MESSAGE, Events.WHISPER, Events.SUBSCRIPTION })
public class General implements EventObserver {

    private static Logger logger = Logger.getLogger(General.class);
    private static Pattern pattern = Pattern
            .compile("^(\\w+) haut €(\\d+)\\.(\\d{2}) raus, Beanie dankt! derindWTF$");

    private static String oauthkey;
    public static String channelid;
    public static Client client;

    static {
        General.oauthkey = ContestBot.getInstance().getConfig("oauth").split(":")[1];

        General.client = ClientBuilder.newClient().register(new Feature() {
            @Override
            public boolean configure(FeatureContext context) {
                context.register(new ClientRequestFilter() {
                    @Override
                    public void filter(ClientRequestContext requestContext) throws IOException {
                        requestContext.getHeaders().add("Authorization", String.format("OAuth %s", oauthkey));
                        if (!requestContext.getHeaders().containsKey("Accept")) {
                            requestContext.getHeaders().add("Accept", "application/vnd.twitchtv.v5+json");
                        }
                    }
                });
                return true;
            }
        });

        General.channelid = getTwitchUserID(ContestBot.getInstance().getConfig("channelname"));
        logger.debug(String.format("Loaded Channelid %s", General.channelid));
    }

    public General() {
        //
    }

    @Override
    public void onEvent(Events type, Event e) {
        boolean whisper = type == Events.WHISPER;
        Message m = (Message) e;

        if (type == Events.SUBSCRIPTION) {
            ContestBot.getInstance().getConnection().sendChatMessage("SUBHYPE <3");
            return;
        }

        if (m.getMessage().charAt(0) == '!') {
            String[] split = m.getMessage().split(" ", 2);
            split[0] = split[0].toLowerCase();
            switch (split[0]) {
                case "!discord": {
                    if (LockHelper.checkAccess("!discord", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "Discord: https://discord.gg/YmvhsxX");
                    }
                    break;
                }
                case "!twitter": {
                    if (LockHelper.checkAccess("!twitter", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "Twitter: https://twitter.com/derinderr");
                    }
                    break;
                }
                case "!ts": {
                    if (LockHelper.checkAccess("!ts", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "TS ist kacke, benutzt lieber !discord!");
                    }
                    break;
                }
                case "!freundin": {
                    if (LockHelper.checkAccess("!freundin", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "Ja -> https://www.twitch.tv/deruuya");
                    }
                    break;
                }
                case "!hardware": {
                    if (LockHelper.checkAccess("!hardware", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "GTX 770 2GB");
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "I5 4690 @ 3,7Ghz");
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "8GB DDR3 RAM");
                    }
                    break;
                }
                case "!uptime": {
                    if (LockHelper.checkAccess("!uptime", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        JsonValue r = General.client.target("https://api.twitch.tv/kraken/streams/")
                                .path(General.channelid).request().get(JsonObject.class).get("stream");
                        if (r.getValueType() == ValueType.OBJECT) {
                            JsonObject stream = (JsonObject) r;
                            Duration d = Duration.between(
                                    ZonedDateTime.parse(stream.getString("created_at"),
                                            DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("Z"))),
                                    ZonedDateTime.now());
                            if (d.getSeconds() / 3600 >= 1) {
                                ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                        String.format("Der Stream ist seit %d Stunden und %d Minuten online",
                                                d.getSeconds() / 3600, d.getSeconds() / 60 % 60));
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
                    if (!whisper && LockHelper.checkAccess("!ripdevil", AuthProvider.checkPrivileged(m.getUsername()),
                            false)) {
                        try {
                            SQLite.getInstance().changeCounter("!ripdevil", 1);
                            ContestBot.getInstance().getConnection().sendChatMessage(
                                    String.format("RIP Devil Count: %d", SQLite.getInstance().getCounter("!ripdevil")));
                        } catch (SQLException e1) {
                            logger.error("Error in !ripdevil", e1);
                        }
                    }
                    break;
                }
                case "!time": {
                    if (LockHelper.checkAccess("!time", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), String.format(
                                "Es ist %s Uhr!", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
                    }
                    break;
                }
                case "!followage": {
                    if (LockHelper.checkAccess("!followage " + m.getUsername(),
                            AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        String userid = null;
                        String username = m.getUsername();
                        if (split.length > 1 && !split[1].isEmpty()) {
                            try {
                                userid = getTwitchUserID(split[1]);
                                username = split[1];
                            } catch (WebApplicationException e1) {
                                ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                        "Der User wurde nicht gefunden!");
                                break;
                            }
                        } else {
                            userid = getTwitchUserID(m.getUsername());
                        }
                        try {
                            JsonObject obj = General.client.target("https://api.twitch.tv/kraken/users").path(userid)
                                    .path("follows/channels").path(General.channelid).request().get(JsonObject.class);
                            Instant followed_at = Instant.parse(obj.getString("created_at"));
                            Duration d = Duration.between(followed_at, Instant.now());
                            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                    String.format("@%s folgt bereits seit %d Tagen %d Stunden und %d Minuten!",
                                            username, d.getSeconds() / 86400, d.getSeconds() / 3600 % 24,
                                            d.getSeconds() / 60 % 60));
                        } catch (WebApplicationException e1) {
                            if (e1.getResponse().getStatus() == 404) {
                                ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                        String.format("@%s ist kein Follower!", username));
                            } else {
                                logger.error("Error getting Followerdata", e1);
                            }
                        }
                    }
                    break;
                }
                case "!sr": {
                	 if (LockHelper.checkAccess("!sr", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                		 ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),"Songrequests: https://musikbot.elite12.de/ (YouTube & Spotify)");
                	 }
                	 break;
                }
                case "!commands": {
                    if (LockHelper.checkAccess("!commands", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        if (AuthProvider.checkPrivileged(m.getUsername())) {
                            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                    "!start [win], !abort, !stop, !judge [win], !adjust, !reset");
                        }
                        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                                "!points, !leaderboard, !verteilung, !teilnehmer, !discord, !twitter, !ts, !hardware, !uptime, !ripdevil, !time, !followage, !freundin, !sr, !watchtime, !commands");
                    }
                    break;
                }
            }
        }
        if (m.getUsername().equalsIgnoreCase(ContestBot.getInstance().getConfig("channelname"))) {
            Matcher matcher = pattern.matcher(m.getMessage());
            if (matcher.matches()) {
                String user = matcher.group(1);
                Integer euro = Integer.parseInt(matcher.group(2));
                Integer cent = Integer.parseInt(matcher.group(3));

                ContestBot.getInstance().getConnection().sendChatMessage(String.format("%s <3", user));
                logger.info(String.format("%s spendet %d.%d€", user, euro, cent));
            }
        }
    }

    private static String getTwitchUserID(String username) throws WebApplicationException {
        WebTarget target = client.target("https://api.twitch.tv/kraken/users").queryParam("login", username);
        JsonArray a = target.request().get(JsonObject.class).getJsonArray("users");
        if (a.size() > 0) {
            return a.getJsonObject(0).getString("_id");
        } else {
            throw new WebApplicationException(404);
        }
    }
}
