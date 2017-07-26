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
				String cmd;
				String s = this.queue.take();
				logger.trace("Handling Message: "+s);
				
				if(s.charAt(0) == ':') {
					String[] split = s.split(" ",3);
					c.setPrefix(split[0]);
					cmd = split[1];
					c.setParams(split.length==3?split[2]:"");
				}
				else {
					String[] split = s.split(" ",2);
					c.setPrefix("");
					cmd = split[0];
					c.setParams(split.length==2?split[1]:"");
				}
				logger.debug("Handling Message: "+c.getPrefix()+"|"+cmd+"|"+c.getParams());
				
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
	
	public final class Command {
		private String prefix,params;

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getParams() {
			return params;
		}

		public void setParams(String params) {
			this.params = params;
		}
				
	}
	//Map of Commands to functions
	private static final Map<String,Consumer<Command>> commandmap;
	static {
		Map<String,Consumer<Command>> map = new HashMap<>();
		
		//Welcome Response
		//Join specified Channel
		map.put("001", (c) -> ContestBot.getInstance().getConnection().send("JOIN #"+ContestBot.getInstance().getConfig("channelname")));
		
		//Inrequested information from the server
		//ignore
		map.put("002", (c) -> {});
		map.put("003", (c) -> {});
		map.put("004", (c) -> {});
		map.put("375", (c) -> {});
		map.put("372", (c) -> {});
		map.put("376", (c) -> {});
		map.put("JOIN", (c) -> logger.info("Successfully joined Channel "+ContestBot.getInstance().getConfig("channelname")));
		map.put("353", (c) -> {});
		map.put("366", (c) -> {});
		
		//Ping from Server
		//Reply with PONG
		map.put("PING", (c) -> ContestBot.getInstance().getConnection().send("PONG :tmi.twitch.tv"));
		
		//TODO:
		//User send message
		//Write to console
		map.put("PRIVMSG", (c) -> {
			String username = c.getPrefix().substring(1).split("@")[0].split("!")[0];
			//Ignoring Channel name, since we currently only support one channel
			String message = c.getParams().split(" :",2)[1];
			logger.info(username + " says: "+message);
		});
		
		commandmap = Collections.unmodifiableMap(map);
	}
}
