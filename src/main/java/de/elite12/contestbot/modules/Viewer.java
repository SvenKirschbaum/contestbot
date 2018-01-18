package de.elite12.contestbot.modules;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.log4j.Logger;

import de.elite12.contestbot.ContestBot;
import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Message;
import de.elite12.contestbot.SQLite;

@Autoload
@EventTypes({ Events.JOIN, Events.PART })
public class Viewer implements EventObserver {
    
    private boolean live = false;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Set<String> viewerset = new HashSet<>();
    
    public Viewer() {
        scheduler.scheduleWithFixedDelay(() -> {
            JsonValue r = General.client.target("https://api.twitch.tv/kraken/streams/").path(General.channelid)
                    .request().get(JsonObject.class).get("stream");
            if (r.getValueType() == ValueType.OBJECT) {
                // LIVE
                if (!this.live) {
                    this.live = true;
                    Logger.getLogger(Viewer.class).info("Channel is now live!");
                }
            } else {
                // Offline
                if (this.live) {
                    this.live = false;
                    Logger.getLogger(Viewer.class).info("Channel is now offline!");
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
        JsonObject viewerobject = General.client.target("https://tmi.twitch.tv/group/user/")
                .path(ContestBot.getInstance().getConfig("channelname")).path("chatters").request()
                .get(JsonObject.class).getJsonObject("chatters");
        JsonArray moderators = viewerobject.getJsonArray("moderators");
        JsonArray staff = viewerobject.getJsonArray("staff");
        JsonArray admins = viewerobject.getJsonArray("admins");
        JsonArray global_mods = viewerobject.getJsonArray("global_mods");
        JsonArray viewers = viewerobject.getJsonArray("viewers");
        
        Consumer<JsonValue> setadd = (v) -> {
            viewerset.add(((JsonString) v).getString().toLowerCase());
        };
        
        moderators.forEach(setadd);
        staff.forEach(setadd);
        admins.forEach(setadd);
        global_mods.forEach(setadd);
        viewers.forEach(setadd);
        
        scheduler.scheduleAtFixedRate(() -> {
            SQLite.getInstance().addView(viewerset);
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void onEvent(Events type, Event e) {
        Message m = (Message) e;
        switch (type) {
            case JOIN: {
                Logger.getLogger(Viewer.class).info("JOIN: " + m.getUsername());
                viewerset.add(m.getUsername().toLowerCase());
                break;
            }
            case PART: {
                Logger.getLogger(Viewer.class).info("LEAVE: " + m.getUsername());
                viewerset.remove(m.getUsername().toLowerCase());
                break;
            }
            default: {
                break;
            }
        }
    }

}
