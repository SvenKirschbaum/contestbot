/*******************************************************************************
 * Copyright (C) 2017 Sven Kirschbaum
 * Copyright (C) 2017 Markus Licht
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.elite12.contestbot.AuthProvider;
import de.elite12.contestbot.ContestBot;
import de.elite12.contestbot.LockHelper;
import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Leaderboard;
import de.elite12.contestbot.Model.Message;
import de.elite12.contestbot.SQLite;

@Autoload
@EventTypes({ Events.MESSAGE, Events.WHISPER })
public class Contest implements EventObserver {
    
    private static final Pattern entrypattern = Pattern.compile("^(\\d{1,2}):(\\d{1,2})$");
    private static final Logger logger = Logger.getLogger(Contest.class);
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private ScheduledFuture<?> bettimer = null;
    private ContestState state;
    
    private static class ContestState implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        ConcurrentHashMap<String, String> map;
        boolean contestrunning = false;
        boolean winonly = false;
        transient boolean open = false;
        
        public ContestState() {
            map = new ConcurrentHashMap<>(100);
        }
        
    }
    
    public Contest() {
        try {
            Path p = Paths.get("contest.state");
            ObjectInputStream in = new ObjectInputStream(Files.newInputStream(p));
            this.state = (ContestState) in.readObject();
            in.close();
        } catch (NoSuchFileException e) {
            logger.info("No state file exists, starting empty");
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Could not load state", e);
        }
        
        if (this.state == null) {
            this.state = new ContestState();
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                saveState();
            }
        }));
    }
    
    public void handleMessage(Message m, boolean whisper) {
        if (this.state.contestrunning && this.state.open) {
            if (this.addEntry(m.getUsername(), m.getMessage()) && whisper) {
                ContestBot.getInstance().getConnection().sendPrivatMessage(m.getUsername().toLowerCase(),
                        String.format("Wette: [%s] confirmed!", m.getMessage()));
            }
        }
        if (m.getMessage().startsWith("!")) {
            String[] split = m.getMessage().split(" ", 2);
            split[0] = split[0].toLowerCase();
            
            // Mod Commands
            if (AuthProvider.checkPrivileged(m.getUsername())) {
                switch (split[0]) {
                    case "!start": {
                        startContest(whisper, m, split.length > 1 ? split[1].equalsIgnoreCase("win") : false);
                        break;
                    }
                    case "!abort": {
                        abortContest(whisper, m);
                        break;
                    }
                    case "!judge": {
                        judgeContest(whisper, m, split.length > 1 ? split[1].equalsIgnoreCase("win") : false);
                        break;
                    }
                    case "!stop": {
                        stopEntries(whisper, m);
                        break;
                    }
                    case "!adjust": {
                        adjustPoints(whisper, m);
                        break;
                    }
                    case "!reset": {
                        resetLeaderboard(whisper, m);
                        break;
                    }
                }
            }
            // User Commands
            switch (split[0]) {
                case "!points": {
                    sendPoints(m.getUsername());
                    break;
                }
                case "!leaderboard": {
                    if (LockHelper.checkAccess("!leaderboard", AuthProvider.checkPrivileged(m.getUsername()),
                            whisper)) {
                        if (whisper) {
                            printLeaderboard(m.getUsername());
                        } else {
                            printLeaderboard();
                        }
                    }
                    break;
                }
                case "!verteilung": {
                    if (LockHelper.checkAccess("!verteilung", AuthProvider.checkPrivileged(m.getUsername()), whisper)) {
                        verteilung(whisper, m);
                    }
                    break;
                }
            }
        }
    }
    
    private void verteilung(boolean whisper, Message m) {
        if (!this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "Es läuft keine Wette");
            return;
        }
        if (this.state.open) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "Die Wette ist noch offen");
            return;
        }
        if (!this.state.winonly) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    "Dieser Wettyp hat keine Verteilung");
            return;
        }
        Set<Entry<String, String>> set = this.state.map.entrySet();
        double size = set.size();
        set.removeIf((e) -> !e.getValue().equalsIgnoreCase("lose"));
        double lose = set.size();
        ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                String.format("Es steht %.2f%% win zu %.2f%% lose", (1 - lose / size) * 100, lose / size * 100));
    }
    
    private synchronized void resetLeaderboard(boolean whisper, Message m) {
        if (this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    "Während einer laufenden Wette kann das Leaderboard nicht resettet werden");
            logger.info("Kann Leaderboard nicht resetten: Wette läuft");
            return;
        }
        try {
            SQLite.getInstance().resetLeaderboard();
            logger.info("Leaderboard resettet");
            ContestBot.getInstance().getConnection().sendChatMessage("Das Leaderboard wurde erfolgreich resettet");
        } catch (SQLException e) {
            logger.error("Resetten des Leaderboards fehlgeschlagen", e);
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    "Resetten des Leaderboards fehlgeschlagen");
        }
    }
    
    private synchronized void startContest(boolean whisper, Message m, boolean winonly) {
        if (this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    "Es läuft bereits eine Wette");
            logger.info("Kann keine Wette starten: läuft bereits");
            return;
        }
        
        this.state.contestrunning = true;
        this.state.open = true;
        this.state.winonly = winonly;
        
        bettimer = scheduler.schedule(() -> {
            this.state.open = false;
            saveState();
            ContestBot.getInstance().getConnection()
                    .sendChatMessage(String.format("Einsendeschluss: %d Teilnehmer", this.state.map.size()));
            logger.info(String.format("Einsendeschluss: %d Teilnehmer", this.state.map.size()));
        }, 3, TimeUnit.MINUTES);
        
        if (winonly) {
            ContestBot.getInstance().getConnection()
                    .sendChatMessage("Eine Wette wurde gestartet: Wird Janu gewinnen oder verlieren?");
            ContestBot.getInstance().getConnection().sendChatMessage("Zum mitwetten schreibt 'win' oder 'lose'");
        } else {
            ContestBot.getInstance().getConnection().sendChatMessage("Eine Wette wurde gestartet: Wann stirbt Janu?");
            ContestBot.getInstance().getConnection()
                    .sendChatMessage("Zum mitwetten schreibt eine Uhrzeit im Format 'HH:mm' oder 'win'");
        }
        ContestBot.getInstance().getConnection().sendChatMessage("Die Einträge schließen in drei Minuten");
        logger.info("Wette gestartet");
    }
    
    private synchronized void abortContest(boolean whisper, Message m) {
        if (!this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "Es läuft keine Wette");
            logger.info("Kann die Wette nicht abbrechen: Es läuft keine Wette");
            return;
        }
        
        if (bettimer != null) {
            bettimer.cancel(false);
        }
        this.state.open = false;
        this.state.contestrunning = false;
        this.state.map.clear();
        
        ContestBot.getInstance().getConnection().sendChatMessage("Die Wette wurde abgebrochen");
        logger.info("Die laufende Wette wurde abgebrochen");
        
        saveState();
    }
    
    private synchronized void judgeContest(boolean whisper, Message m, boolean win) {
        if (!this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "Es läuft keine Wette");
            logger.info("Kann die Wette nicht beenden: Es läuft keine Wette");
            return;
        }
        if (this.state.open) {
            bettimer.cancel(false);
        }
        
        if (win) {
            Set<Entry<String, String>> set = this.state.map.entrySet();
            set.removeIf((e) -> !e.getValue().equalsIgnoreCase("win"));
            
            Set<String> winset = new HashSet<>();
            set.forEach((e) -> winset.add(e.getKey()));
            handleWinner(winset, Duration.ZERO, win);
        } else {
            if (this.state.winonly) {
                Set<Entry<String, String>> set = this.state.map.entrySet();
                set.removeIf((e) -> !e.getValue().equalsIgnoreCase("lose"));
                
                Set<String> winset = new HashSet<>();
                set.forEach((e) -> winset.add(e.getKey()));
                handleWinner(winset, Duration.ZERO, win);
            } else {
                Set<Entry<String, String>> set = this.state.map.entrySet();
                LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
                Duration d = Duration.ofDays(1);
                Set<String> winset = new HashSet<>();

                set.removeIf((e) -> e.getValue().equalsIgnoreCase("win"));
                for (Entry<String, String> e : set) {
                    Duration diff = Duration
                            .between(LocalDateTime.parse(e.getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME), now)
                            .abs();
                    int result = diff.compareTo(d);
                    if (result == 0) {
                        winset.add(e.getKey());
                    }
                    if (result < 0) {
                        winset.clear();
                        winset.add(e.getKey());
                        d = diff;
                    }
                }
                handleWinner(winset, d, win);
            }
        }
        
        this.state.contestrunning = false;
        this.state.map.clear();
        saveState();
    }
    
    private synchronized void stopEntries(boolean whisper, Message m) {
        if (!this.state.contestrunning) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(), "Es läuft keine Wette");
            logger.info("Kann die Einträge nicht beenden: Es läuft keine Wette");
            return;
        }
        if (!this.state.open) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, m.getUsername(),
                    "Die Wette ist bereits geschlossen");
            logger.info("Die Wette ist bereits geschlossen");
            return;
        }
        
        if (bettimer != null) {
            bettimer.cancel(false);
        }
        this.state.open = false;
        
        ContestBot.getInstance().getConnection().sendChatMessage(String
                .format("Die Einsendungen wurden vorzeitig beendet, es gab %d Teilnehmer", this.state.map.size()));
        logger.info(String.format("Die Einsendungen wurden vorzeitig beendet, es gab %d Teilnehmer",
                this.state.map.size()));
        saveState();
    }
    
    private void adjustPoints(boolean whisper, Message message) {
        String[] split = message.getMessage().split(" ");
        if (split.length != 3) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, message.getUsername(), "Ungültige Parameter");
            return;
        }
        String username = split[1];
        Integer points;
        try {
            points = Integer.parseInt(split[2]);
        } catch (NumberFormatException e) {
            ContestBot.getInstance().getConnection().sendMessage(whisper, message.getUsername(), "Ungültige Parameter");
            return;
        }
        
        try {
            SQLite.getInstance().changePoints(username, points);
        } catch (SQLException e) {
            logger.error("Cant change points", e);
            return;
        }
        ContestBot.getInstance().getConnection().sendMessage(whisper, message.getUsername(),
                String.format("Punkte von %s um %d geändert", username, points));
        logger.info(String.format("Punkte von %s um %d geändert", username, points));
    }
    
    private void sendPoints(String username) {
        logger.debug("Sending points to " + username);
        try {
            int points = SQLite.getInstance().getPoints(username);
            if (points != 1) {
                ContestBot.getInstance().getConnection().sendPrivatMessage(username,
                        String.format("Du hast aktuell %d Punkte!", points));
            } else {
                ContestBot.getInstance().getConnection().sendPrivatMessage(username, "Du hast aktuell einen Punkt!");
            }
        } catch (SQLException e) {
            logger.error("Unable to get Points", e);
        }
    }
    
    private void printLeaderboard() {
        try {
            Leaderboard l = SQLite.getInstance().getLeaderboard(5);
            ContestBot.getInstance().getConnection().sendChatMessage("Die Top 5:");
            for (int i = 0; i < l.getUsernames().length; i++) {
                ContestBot.getInstance().getConnection().sendChatMessage(
                        String.format("%d. %s: %d Punkte", i + 1, l.getUsernames()[i], l.getPoints()[i]));
            }
        } catch (SQLException e) {
            logger.error("Could not get Leaderboard", e);
        }
    }
    
    private void printLeaderboard(String username) {
        try {
            Leaderboard l = SQLite.getInstance().getLeaderboard(5);
            ContestBot.getInstance().getConnection().sendPrivatMessage(username, "Die Top 5:");
            for (int i = 0; i < l.getUsernames().length; i++) {
                final int j = i;
                scheduler.schedule(() -> {
                    ContestBot.getInstance().getConnection().sendPrivatMessage(username,
                            String.format("%d. %s: %d Punkte", j + 1, l.getUsernames()[j], l.getPoints()[j]));
                }, i + 1, TimeUnit.SECONDS);
            }
        } catch (SQLException e) {
            logger.error("Could not get Leaderboard", e);
        }
    }
    
    private boolean addEntry(String username, String message) {
        Matcher m = entrypattern.matcher(message);
        if (m.matches() && !this.state.winonly) {
            int hours = Integer.parseInt(m.group(1));
            int minutes = Integer.parseInt(m.group(2));
            
            if (hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59) {
                LocalDateTime now = LocalDateTime.now().minusMinutes(2);
                LocalDateTime entrytime = now.withMinute(minutes).withHour(hours).withSecond(0).withNano(0);
                if (entrytime.isBefore(now)) {
                    entrytime = entrytime.plusDays(1);
                }
                
                this.state.map.put(username, entrytime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            return true;
        } else if (message.equalsIgnoreCase("win")) {
            this.state.map.put(username, "win");
            return true;
        } else if (message.equalsIgnoreCase("lose") && this.state.winonly) {
            this.state.map.put(username, "lose");
            return true;
        } else {
            return false;
        }
    }
    
    private void handleWinner(Set<String> set, Duration d, boolean win) {
        logger.debug(d + "|" + LocalDateTime.now());
        switch (set.size()) {
            case 0: {
                ContestBot.getInstance().getConnection().sendChatMessage("Leider gibt es keinen Gewinner :(");
                logger.info("Die Wette wurde beendet, es gibt keinen Gewinner");
                break;
            }
            case 1: {
                String winner = set.iterator().next();
                ContestBot.getInstance().getConnection()
                        .sendChatMessage(String.format("Der Gewinner ist %s <3", winner));
                if (!this.state.winonly) {
                    printDuration(d);
                }
                logger.info(String.format("Die Wette wurde beendet, %s hat gewonnen", winner));
                break;
            }
            default: {
                StringBuilder s = new StringBuilder();
                for (String e : set) {
                    s.append(e);
                    s.append(',');
                }
                s.deleteCharAt(s.length() - 1);
                
                ContestBot.getInstance().getConnection()
                        .sendChatMessage(String.format("Gewonnen haben: %s <3", s.toString()));
                logger.info(String.format("Die Wette wurde beendet, %s haben gewonnen", s.toString()));
                if (!this.state.winonly) {
                    printDuration(d);
                }
            }
        }
        // Punkte
        for (String winner : set) {
            try {
                if (this.state.winonly) {
                    SQLite.getInstance().changePoints(winner, 1);
                } else {
                    if (win) {
                        SQLite.getInstance().changePoints(winner, 10);
                    } else {
                        SQLite.getInstance().changePoints(winner,
                                (int) (d.getSeconds() / 60 <= 4 ? 5 - d.getSeconds() / 60 : 1));
                    }
                }
            } catch (SQLException e) {
                logger.error("Could not add Points", e);
            }
        }
    }
    
    private void printDuration(Duration d) {
        int abstand = (int) (d.getSeconds() / 60);
        switch (abstand) {
            case 0: {
                ContestBot.getInstance().getConnection().sendChatMessage("Es war ein Volltreffer!");
                break;
            }
            case 1: {
                ContestBot.getInstance().getConnection().sendChatMessage("Der Abstand beträgt nur eine Minute!");
                break;
            }
            default: {
                ContestBot.getInstance().getConnection()
                        .sendChatMessage(String.format("Der Abstand beträgt %d Minuten", abstand));
            }
        }
    }
    
    @Override
    public void onEvent(Events type, Event e) {
        this.handleMessage((Message) e, type == Events.WHISPER);
    }
    
    private void saveState() {
        synchronized (state) {
            try {
                Path p = Paths.get("contest.state");
                Files.deleteIfExists(p);
                ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(p));
                out.writeObject(state);
                out.flush();
                out.close();
            } catch (IOException e) {
                logger.error("Could not save state", e);
            }
        }
    }
}
