package de.elite12.contestbot;

import java.util.Map;

public class Model {
	public final static class Command {
		private String prefix,params;
		private Map<String,String> tags = null;

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
		
		public void setTags(Map<String,String> t) {
			this.tags = t;
		}
		
		public Map<String,String> getTags() {
			return tags;
		}
				
	}
	public final static class Message {
		private String username,message;
		private Map<String,String> tags = null;

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
		
		public void setTags(Map<String,String> t) {
			this.tags = t;
		}
		
		public Map<String,String> getTags() {
			return tags;
		}

		@Override
		public String toString() {
			return username + " says: "+message;
		}
	}
}
