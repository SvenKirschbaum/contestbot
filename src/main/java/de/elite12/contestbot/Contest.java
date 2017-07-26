package de.elite12.contestbot;

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

import de.elite12.contestbot.Model.Message;


public class Contest {
	
	private static final Pattern entrypattern = Pattern.compile("^(\\d{1,2}):(\\d{1,2})$");
	private static final Logger logger = Logger.getLogger(Contest.class);
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	boolean contestrunning = false;
	boolean open = false;
	ScheduledFuture<?> timer = null;
	ConcurrentHashMap<String, String> map;
	
	public Contest() {
		map = new ConcurrentHashMap<>(100);
	}
	
	public void handleMessage(Message m) {
		if(contestrunning && open) {
			this.addEntry(m.getUsername(),m.getMessage());
		}
		if(m.getMessage().startsWith("!") && this.ispermitted(m)) {
			String[] split = m.getMessage().split(" ", 2);
			
			switch(split[0]) {
				case "!start": {
					startContest();
					break;
				}
				case "!abort": {
					abortContest();
					break;
				}
				case "!judge": {
					judgeContest(split.length>1?split[1].equalsIgnoreCase("win"):false);
					break;
				}
			}
		}
	}
	
	private void startContest() {
		if(contestrunning) {
			ContestBot.getInstance().getConnection().sendChatMessage("Es läuft bereits eine Wette");
			logger.info("Kann keine Wette starten: läuft bereits");
			return;
		}
		
		this.contestrunning = true;
		this.open = true;
		
		timer = scheduler.schedule(() -> {
			this.open = false;
			ContestBot.getInstance().getConnection().sendChatMessage("Die Einträge sind nun abgeschlossen");
			logger.info("Die Einträge sind nun abgeschlossen");
		}, 3, TimeUnit.MINUTES);
		
		ContestBot.getInstance().getConnection().sendChatMessage("Eine Wette wurde gestartet");
		ContestBot.getInstance().getConnection().sendChatMessage("Zum mitwetten schreibt eine Uhrzeit im Format 'HH:mm' oder 'win'");
		ContestBot.getInstance().getConnection().sendChatMessage("Die Einträge schließen in drei Minuten");
		logger.info("Wette gestartet");
	}
	
	private void abortContest() {
		if(!contestrunning) {
			ContestBot.getInstance().getConnection().sendChatMessage("Es läuft keine Wette");
			logger.info("Kann die Wette nicht abbrechen: Es läuft keine Wette");
			return;
		}
		
		if(timer != null) timer.cancel(false);
		this.open = false;
		this.contestrunning = false;
		this.map.clear();
		
		ContestBot.getInstance().getConnection().sendChatMessage("Die Wette wurde abgebrochen");
		logger.info("Die laufende Wette wurde abgebrochen");
	}
	
	private void judgeContest(boolean b) {
		if(!contestrunning) {
			ContestBot.getInstance().getConnection().sendChatMessage("Es läuft keine Wette");
			logger.info("Kann die Wette nicht beenden: Es läuft keine Wette");
			return;
		}
		if(open) {
			timer.cancel(false);
		}
		
		if(b) {
			Set<Entry<String, String>> set = this.map.entrySet();
			set.removeIf((e) -> !e.getValue().equalsIgnoreCase("win"));
			
			Set<String> winset = new HashSet<>();
			set.forEach((e) -> winset.add(e.getKey()));
			printWinner(winset,Duration.ZERO);
		}
		else {
			Set<Entry<String, String>> set = this.map.entrySet();
			LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
			Duration d = Duration.ofDays(1);
			Set<String> winset = new HashSet<>();
			
			set.removeIf((e) -> e.getValue().equalsIgnoreCase("win"));
			for(Entry<String,String> e:set) {
				Duration diff = Duration.between(LocalDateTime.parse(e.getValue(), DateTimeFormatter.ISO_LOCAL_DATE_TIME), now).abs();
				int result = diff.compareTo(d);
				if(result == 0) {
					winset.add(e.getKey());
				}
				if(result < 0) {
					winset.clear();
					winset.add(e.getKey());
					d=diff;
				}
			}
			
			printWinner(winset,d);
		}
		
		this.contestrunning = false;
		this.map.clear();
	}

	private boolean ispermitted(Message m) {
		return m.getTags().containsKey("mod")?m.getTags().get("mod").equals("1"):false;
	}

	private void addEntry(String username, String message) {
		Matcher m = entrypattern.matcher(message);
		if(m.matches()) {
			int hours = Integer.parseInt(m.group(1));
			int minutes = Integer.parseInt(m.group(2));
			
			if(hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59) {
				LocalDateTime now = LocalDateTime.now().minusMinutes(2);
				LocalDateTime entrytime = now.withMinute(minutes).withHour(hours).withSecond(0).withNano(0);
				if(entrytime.isBefore(now)) entrytime = entrytime.plusDays(1);
				
				this.map.put(username,entrytime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			}
		}
		else if(message.equalsIgnoreCase("win")) this.map.put(username, "win");
		
	}
	
	private void printWinner(Set<String> set,Duration d) {
		logger.debug(d + "|" + LocalDateTime.now());
		switch(set.size()) {
			case 0: {
				ContestBot.getInstance().getConnection().sendChatMessage("Leider gibt es keinen Gewinner :(");
				break;
			}
			case 1: {
				ContestBot.getInstance().getConnection().sendChatMessage("Der Gewinner ist " + set.iterator().next() + " <3");
				printDuration(d);
				break;
			}
			default: {
				StringBuilder s = new StringBuilder();
				for(String e:set) {
					s.append(e);
					s.append(',');
				}
				s.deleteCharAt(s.length()-1);
				
				ContestBot.getInstance().getConnection().sendChatMessage("Gewonnen haben: " + s.toString() + " <3");
				printDuration(d);
			}
		}
	}
	private void printDuration(Duration d) {
		int abstand = (int) (d.getSeconds()/60);
		switch(abstand) {
			case 0: {
				ContestBot.getInstance().getConnection().sendChatMessage("Es war ein Volltreffer!");
				break;
			}
			case 1: {
				ContestBot.getInstance().getConnection().sendChatMessage("Der Abstand beträgt nur eine Minute!");
				break;
			}
			default: {
				ContestBot.getInstance().getConnection().sendChatMessage("Der Abstand beträgt "+abstand+" Minuten");
			}
		}
	}
}
