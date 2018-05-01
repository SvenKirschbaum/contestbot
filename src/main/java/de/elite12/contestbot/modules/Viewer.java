package de.elite12.contestbot.modules;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
@EventTypes({ Events.JOIN, Events.PART, Events.MESSAGE, Events.WHISPER })
public class Viewer implements EventObserver {

    private boolean live = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Set<String> viewerset = new HashSet<>();
    private ViewerData viewerdata;

    public Viewer() {
        // Load Viewerdata
        try {
            Path p = Paths.get("viewer.state");
            ObjectInputStream in = new ObjectInputStream(Files.newInputStream(p));
            this.viewerdata = (ViewerData) in.readObject();
            in.close();
        } catch (NoSuchFileException e) {
            Logger.getLogger(Viewer.class).info("No state file exists, starting empty");
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(Viewer.class).error("Could not load viewerdata", e);
        }
        
        if (this.viewerdata == null) {
            this.viewerdata = new ViewerData();
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                saveState();
            }
        }));
        
        
        scheduler.scheduleWithFixedDelay(() -> {
             JsonObject g = General.client.target("https://api.twitch.tv/kraken/streams/").path(General.channelid)
                    .request().get(JsonObject.class);
             JsonValue r = g.get("stream");
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
        
        
        scheduler.submit(() -> {
        	// Load current viewers
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

            viewerset.remove(ContestBot.getInstance().getConfig("login").toLowerCase());
            viewerset.remove(ContestBot.getInstance().getConfig("channelname").toLowerCase());
        });
        
        // Schedule adding viewtime
        scheduler.scheduleAtFixedRate(() -> {
            addViewTime();
        }, 1, 1, TimeUnit.MINUTES);

        scheduler.scheduleAtFixedRate(() -> {
            saveState();
        }, 15, 15, TimeUnit.MINUTES);
    }
    
    @Override
    public void onEvent(Events type, Event e) {
        Message m = (Message) e;
        switch (type) {
            case JOIN: {
                Logger.getLogger(Viewer.class).debug("JOIN: " + m.getUsername());
                if (!m.getUsername().equalsIgnoreCase(ContestBot.getInstance().getConfig("login"))
                        && !m.getUsername().equalsIgnoreCase(ContestBot.getInstance().getConfig("channelname"))) {
                    viewerset.add(m.getUsername().toLowerCase());
                }
                break;
            }
            case PART: {
                Logger.getLogger(Viewer.class).debug("LEAVE: " + m.getUsername());
                viewerset.remove(m.getUsername().toLowerCase());
                break;
            }
            case MESSAGE:
            case WHISPER: {
                handleMessage(type == Events.WHISPER, m);
                break;
            }
            default: {
                break;
            }
        }
    }
    
    private void handleMessage(boolean whisper, Message m) {
        if (m.getMessage().startsWith("!")) {
            String[] split = m.getMessage().split(" ", 2);
            split[0] = split[0].toLowerCase();

            // Mod Commands
            if (AuthProvider.checkPrivileged(m.getUsername())) {
                switch (split[0]) {
                    default: {
                        break;
                    }
                }
            }
            // User Commands
            switch (split[0]) {
                case "!watchtime": {
                    if (LockHelper.checkAccess("!watchtime " + m.getUsername(),
                            AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        sendViewTime(whisper, m);
                        break;
                    }
                }
            }
        }
    }

    private void sendViewTime(boolean whisper, Message m) {
        String username = m.getUsername();
        String[] split = m.getMessage().split(" ", 2);
        if (split.length > 1 && !split[1].isEmpty()) {
            username = split[1];
        }
        
        Integer t = this.viewerdata.map.get(username);
        if (t == null) {
            t = 0;
        }
        Duration d = Duration.ofMinutes(t.longValue());
        if (d.getSeconds() / 3600 >= 24) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    String.format("@%s hat bereits %d Tage, %d Stunden und %d Minuten bei Beanie verschwendet",
                            username, d.getSeconds() / 86400, d.getSeconds() / 3600 % 24, d.getSeconds() / 60 % 60));
        } else if (d.getSeconds() / 3600 >= 1) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    String.format("@%s hat bereits %d Stunden und %d Minuten bei Beanie verschwendet", username,
                            d.getSeconds() / 3600, d.getSeconds() / 60 % 60));
        } else {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    String.format("@%s hat bereits %d Minuten bei Beanie verschwendet", username, d.getSeconds() / 60));
        }
    }
    
    private void addViewTime() {
        if (live) {
            this.viewerset.forEach((s) -> {
                this.viewerdata.map.putIfAbsent(s, 0);
                this.viewerdata.map.compute(s, (k, v) -> {
                    int i = v + 1;
                    if (i % 60 == 0) {
                        try {
                            SQLite.getInstance().changePoints(k, 1);
                        } catch (SQLException e) {
                            Logger.getLogger(this.getClass()).error("Error adding Viewerpoints", e);
                        }
                    }
                    return i;
                });
            });
        }
    }
    
    private void saveState() {
        synchronized (viewerdata) {
            try {
                Path p = Paths.get("viewer.state");
                Files.deleteIfExists(p);
                ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(p));
                out.writeObject(viewerdata);
                out.flush();
                out.close();
            } catch (IOException e) {
                Logger.getLogger(Viewer.class).error("Could not save state", e);
            }
        }
    }

    private static class ViewerData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        ConcurrentHashMap<String, Integer> map;
        
        public ViewerData() {
            this.map = new ConcurrentHashMap<>();
        }
    }
}
