package de.elite12.contestbot.modules;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;

@Autoload
@EventTypes({Events.SUBSCRIPTION, Events.HOST, Events.TIMEOUT, Events.MESSAGE})
public class ChatLog implements EventObserver {
	
	private PrintWriter out;
	private ScheduledExecutorService scheduler;
	
	public ChatLog() {
		try {
			if(!Files.exists(Paths.get("chatlog.txt")))
				Files.createFile(Paths.get("chatlog.txt"));
			this.out = new PrintWriter(Files.newBufferedWriter(Paths.get("chatlog.txt"), StandardOpenOption.APPEND));
		}
		catch (IOException e) {
			throw new RuntimeException("Could not open chatlog file", e);
		}
		
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			out.flush();
		}, 5, 5, TimeUnit.SECONDS);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			scheduler.shutdownNow();
			out.flush();
			out.close();
		}));
	}

	@Override
	public void onEvent(Events type, Event e) {
		this.out.println(String.format("[%tc] [%s] %s", ZonedDateTime.now(), type.toString() ,e.toString()));
	}

}
