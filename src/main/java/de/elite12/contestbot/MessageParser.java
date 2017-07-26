package de.elite12.contestbot;

import java.nio.BufferOverflowException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import de.elite12.contestbot.Model.Command;
import de.elite12.contestbot.Model.Message;

public class MessageParser extends Thread {
	private static Logger logger = Logger.getLogger(MessageParser.class);
	private BlockingQueue<String> queue;
	public MessageParser() {
		this.queue = new ArrayBlockingQueue<>(500);
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				Command c = new Command();
				String cmd = "";
				String s = this.queue.take();
				logger.trace("Handling Message: "+s);
				
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
				logger.debug("Handling Message: " + c.getTags() + "|"+c.getPrefix()+"|"+cmd+"|"+c.getParams());
				
				Consumer<Command> action = MessageParser.commandmap.get(cmd);
				if(action != null) {
					action.accept(c);
				}
				else {
					logger.warn("No mapping for command: "+cmd);
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

	public void queueElement(String e) throws BufferOverflowException{
		try {
			if(!this.queue.offer(e, 500, TimeUnit.MILLISECONDS)) {
				throw new BufferOverflowException();
			}
		} catch (InterruptedException e1) {
			logger.warn("Got interruted while queuing item",e1);
			throw new BufferOverflowException();
		}
	}
	
	//Map of Commands to functions
	private static final Map<String,Consumer<Command>> commandmap;
	static {
		Map<String,Consumer<Command>> map = new HashMap<>();
		
		Consumer<Command> donothing = (c) -> {};
		
		//Welcome Response
		//Request Tags functionality and join specified Channel
		map.put("001", (c) -> {
			ContestBot.getInstance().getConnection().send("CAP REQ :twitch.tv/tags");
			ContestBot.getInstance().getConnection().send("JOIN #"+ContestBot.getInstance().getConfig("channelname"));
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
		map.put("JOIN", (c) -> logger.info("Successfully joined Channel "+ContestBot.getInstance().getConfig("channelname")));
		map.put("353", donothing);
		map.put("366", donothing);
		//Requested CAP
		map.put("CAP", donothing);
		
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
			
			//TODO: Dynamic observers
			ContestBot.getInstance().getContest().handleMessage(m);
		});
		
		commandmap = Collections.unmodifiableMap(map);
	}
}
