package de.elite12.contestbot;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Map;

public class Model {
	public final static class Command {
		private String prefix, params;
		private Map<String, String> tags = null;

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

		public void setTags(Map<String, String> t) {
			this.tags = t;
		}

		public Map<String, String> getTags() {
			return tags;
		}

	}

	public final static class Message implements Event {
		private String username, message;
		private Map<String, String> tags = null;

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public void setTags(Map<String, String> t) {
			this.tags = t;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		@Override
		public String toString() {
			return username + " says: " + message;
		}
	}

	public final static class Leaderboard {
		private String[] usernames;
		private Integer[] points;

		public String[] getUsernames() {
			return usernames;
		}

		public void setUsernames(String[] usernames) {
			this.usernames = usernames;
		}

		public Integer[] getPoints() {
			return points;
		}

		public void setPoints(Integer[] points) {
			this.points = points;
		}
	}

	//Observable Event types
	public static enum Events {
		MESSAGE, WHISPER
	}

	//implemented by the model class of every observable event
	public static interface Event {};
	
	//implemented by classes that want to observ events
	public static interface EventObserver {
		public void onEvent(Events type, Event e);
	}
	
	//used internally to save registered observers
	public static class EventObserverEntry {
		public EventObserver observer;
		public EnumSet<Events> types;
		
		public EventObserverEntry(EnumSet<Events> types, EventObserver obs) {
			this.observer = obs;
			this.types = types;
		}
	}
	
	//Mark class for automated creation and registration as observer
	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface Autoload {}
	
	@Target(TYPE)
	@Retention(RUNTIME)
	public static @interface EventTypes {
		Events[] value();
	}
}
