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
package de.elite12.contestbot;

import java.nio.BufferOverflowException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import de.elite12.contestbot.Model.Command;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventObserverEntry;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Host;
import de.elite12.contestbot.Model.Message;
import de.elite12.contestbot.Model.Notice;
import de.elite12.contestbot.Model.Timeout;

public class MessageParser implements Runnable {
	private static Logger logger = Logger.getLogger(MessageParser.class);
	private static Set<EventObserverEntry> observers = new HashSet<>();
	protected static BlockingQueue<String> queue = new ArrayBlockingQueue<>(500);
	
	@Override
	public void run() {
		while(true) {
			try {
				Command c = new Command();
				String cmd = "";
				String s = MessageParser.queue.take();
				if(logger.isTraceEnabled())
					logger.trace(String.format("Handling Message: %s", s));
				
				int parts;
				switch(s.charAt(0)) {
					case '@': {
						parts = 4;
						break;
					}
					case ':': {
						parts = 3;
						break;
					}
					default: {
						parts = 2;
					}
				}
				
				String[] split = s.split(" ", parts);
				
				switch(parts) {
					case 4: {
						c.setTags(this.parseTags(split[0].substring(1)));
						c.setPrefix(split[1].substring(1));
						cmd=split[2];
						c.setParams(split.length==4?split[3]:"");
						break;
					}
					case 3: {
						c.setTags(null);
						c.setPrefix(split[0].substring(1));
						cmd=split[1];
						c.setParams(split.length==3?split[2]:"");
						break;
					}
					case 2: {
						c.setTags(null);
						c.setPrefix("");
						cmd=split[0];
						c.setParams(split.length==2?split[1]:"");
						break;
					}
				}
				if(logger.isDebugEnabled())
					logger.debug(String.format("Handling Message: %s|%s|%s|%s", c.getTags(), c.getPrefix(), cmd, c.getParams()));
				
				Consumer<Command> action = MessageParser.commandmap.get(cmd);
				if(action != null) {
					action.accept(c);
				}
				else {
					logger.warn(String.format("No mapping for command: %s", cmd));
				}
				
			} catch (InterruptedException e) {
				logger.debug("Interrupted while waiting for Message", e);
			} catch(RuntimeException e) {
				logger.error("Error parsing Message", e);
			}
		}
	}
	
	private Map<String, String> parseTags(String string) {
		Map<String,String> map = new HashMap<>();
		String[] split = string.split(";|=");
		
		for(int i = 0;i<(split.length/2);i++) {
			map.put(split[i], split[++i]);
		}
		
		return map;
	}

	public static void queueElement(String e) throws BufferOverflowException{
		try {
			if(!queue.offer(e, 500, TimeUnit.MILLISECONDS)) {
				throw new BufferOverflowException();
			}
		} catch (InterruptedException e1) {
			logger.warn("Got interruted while queuing item",e1);
			throw new BufferOverflowException();
		}
	}
	
	////Static part
	
	//Map of Commands to functions
	private static final Map<String,Consumer<Command>> commandmap;
	static {
		Map<String,Consumer<Command>> map = new HashMap<>();
		
		Consumer<Command> donothing = (c) -> {};
		
		//Welcome Response
		//Request Tags functionality and join specified Channel
		map.put("001", (c) -> {
			ContestBot.getInstance().getConnection().send("CAP REQ :twitch.tv/tags");
			ContestBot.getInstance().getConnection().send("CAP REQ :twitch.tv/commands");
			ContestBot.getInstance().getConnection().send(String.format("JOIN #%s", ContestBot.getInstance().getConfig("channelname")));
		});
		
		//Unrequested information from the server
		//ignore
		map.put("002", donothing);
		map.put("003", donothing);
		map.put("004", donothing);
		map.put("375", donothing);
		map.put("372", donothing);
		map.put("376", donothing);
		//Confirmed Join
		map.put("JOIN", (c) -> logger.info(String.format("Successfully joined Channel %s", ContestBot.getInstance().getConfig("channelname"))));
		map.put("353", donothing);
		map.put("366", donothing);
		//Confirmed Requested CAP, we dont check this (yet)
		map.put("CAP", donothing);
		//Unknown Command
		map.put("421", (c) -> {
			logger.error("Unknown command");
		});
		
		//Ignoring userstate
		map.put("USERSTATE", donothing);
		
		//RECONNECT
		map.put("RECONNECT", (c) -> {
			ContestBot.getInstance().reconnect();
		});
		
		//Roomstate
		map.put("ROOMSTATE", (c) -> {
			notifyObservers(Events.ROOMSTATE,c);
		}); 
		
		//On subscription to a channel.
		map.put("USERNOTICE", (c) -> {
			Message m = new Message();
			
			m.setTags(c.getTags());
			m.setUsername(c.getTags().get("login"));
			m.setMessage(c.getParams().split(" :",2).length == 2 ? c.getParams().split(" :",2)[1]:"");
			
			notifyObservers(Events.SUBSCRIPTION, m);
		}); 
		
		//Host starts or stops a message.
		map.put("HOSTTARGET", (c) -> {
			Host h = new Host();
			
			h.setViewers(c.getParams().split(" ").length==3?c.getParams().split(" ")[2]:"");
			h.setTarget(c.getParams().split(" ")[1].substring(1));
			
			notifyObservers(Events.HOST, h);
		}); 
		
		//Temporary or permanent ban on a channel.
		map.put("CLEARCHAT", (c) -> {
			Timeout t = new Timeout();
			
			if(c.getTags() != null && c.getTags().containsKey("ban-duration")) {
				t.setDuration(Long.parseLong(c.getTags().get("ban-duration")));
			}
			else {
				t.setDuration(0L);
			}
			t.setReason(c.getTags()!=null?c.getTags().get("ban-reason"):null);
			t.setUser(c.getParams().split(" :").length>=2?c.getParams().split(" :")[1]:"");
			
			notifyObservers(Events.TIMEOUT, t);
		}); 
		
		//General notices from the server.
		map.put("NOTICE", (c) -> {
			Notice n = new Notice();
			n.setMsgid(c.getTags()!=null?c.getTags().get("msg-id"):null);
			n.setChannel(c.getParams().split(" :",2)[0]);
			n.setMessage(c.getParams().split(" :",2)[1]);
			
			if(n.getMsgid()==null)
				logger.warn(n);
			notifyObservers(Events.NOTICE, n);
		}); 
		
		//Ping from Server
		//Reply with PONG
		map.put("PING", (c) -> ContestBot.getInstance().getConnection().send("PONG :tmi.twitch.tv"));
		
		//User sends message
		//Write to console
		map.put("PRIVMSG", (c) -> {
			Message m = new Message();
			m.setUsername(c.getPrefix().split("@")[0].split("!")[0]);
			//Ignoring Channel name, since we currently only support one channel
			m.setMessage(c.getParams().split(" :",2)[1]);
			m.setTags(c.getTags());
			
			notifyObservers(Events.MESSAGE, m);
		});
		map.put("WHISPER", (c) -> {
			Message m = new Message();
			m.setUsername(c.getPrefix().split("@")[0].split("!")[0]);
			//Ignoring Channel name, since whispers have no channel
			m.setMessage(c.getParams().split(" :",2)[1]);
			m.setTags(c.getTags());
			
			notifyObservers(Events.WHISPER, m);
		});
		
		commandmap = Collections.unmodifiableMap(map);
	}
	
	protected static void notifyObservers(Events type, Event e) {
		for(EventObserverEntry entry:observers) {
			if(entry.types.contains(type))
				entry.observer.onEvent(type, e);
		}
	}
	
	public static void registerObserver(EnumSet<Events> types, EventObserver observer) {
		observers.add(new EventObserverEntry(types, observer));
	}
}
