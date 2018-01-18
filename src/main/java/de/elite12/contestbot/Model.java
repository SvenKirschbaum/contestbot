/*******************************************************************************
 * Copyright (C) 2017 Sven Kirschbaum
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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

public class Model {
    public final static class Command implements Event {
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
        
        @Override
        public String toString() {
            return String.format("Command [prefix=%s, params=%s, tags=%s]", prefix, params, tags);
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (params == null ? 0 : params.hashCode());
            result = prime * result + (prefix == null ? 0 : prefix.hashCode());
            result = prime * result + (tags == null ? 0 : tags.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Command other = (Command) obj;
            if (params == null) {
                if (other.params != null) {
                    return false;
                }
            } else if (!params.equals(other.params)) {
                return false;
            }
            if (prefix == null) {
                if (other.prefix != null) {
                    return false;
                }
            } else if (!prefix.equals(other.prefix)) {
                return false;
            }
            if (tags == null) {
                if (other.tags != null) {
                    return false;
                }
            } else if (!tags.equals(other.tags)) {
                return false;
            }
            return true;
        }
        
    }
    
    public final static class Message implements Event {
        private String username, message;
        private Map<String, String> tags;
        
        public Message() {
            this.username = null;
            this.message = null;
            this.tags = null;
        }
        
        public Message(String username, String message, Map<String, String> tags) {
            this.username = username;
            this.message = message;
            this.tags = tags;
        }
        
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
            return String.format("%s says: %s [%s]", username, message, tags != null ? tags.toString() : "null");
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (message == null ? 0 : message.hashCode());
            result = prime * result + (tags == null ? 0 : tags.hashCode());
            result = prime * result + (username == null ? 0 : username.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Message other = (Message) obj;
            if (message == null) {
                if (other.message != null) {
                    return false;
                }
            } else if (!message.equals(other.message)) {
                return false;
            }
            if (tags == null) {
                if (other.tags != null) {
                    return false;
                }
            } else if (!tags.equals(other.tags)) {
                return false;
            }
            if (username == null) {
                if (other.username != null) {
                    return false;
                }
            } else if (!username.equals(other.username)) {
                return false;
            }
            return true;
        }
    }
    
    public final static class Notice implements Event {
        private String msgid, channel, message;
        
        public Notice() {
            this.msgid = null;
            this.channel = null;
            this.message = null;
        }
        
        public Notice(String msgid, String channel, String message) {
            this.msgid = msgid;
            this.channel = channel;
            this.message = message;
        }
        
        public String getMsgid() {
            return msgid;
        }
        
        public void setMsgid(String msgid) {
            this.msgid = msgid;
        }
        
        public String getChannel() {
            return channel;
        }
        
        public void setChannel(String channel) {
            this.channel = channel;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        @Override
        public String toString() {
            return String.format("Notice [msgid=%s, channel=%s, message=%s]", msgid, channel, message);
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (channel == null ? 0 : channel.hashCode());
            result = prime * result + (message == null ? 0 : message.hashCode());
            result = prime * result + (msgid == null ? 0 : msgid.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Notice other = (Notice) obj;
            if (channel == null) {
                if (other.channel != null) {
                    return false;
                }
            } else if (!channel.equals(other.channel)) {
                return false;
            }
            if (message == null) {
                if (other.message != null) {
                    return false;
                }
            } else if (!message.equals(other.message)) {
                return false;
            }
            if (msgid == null) {
                if (other.msgid != null) {
                    return false;
                }
            } else if (!msgid.equals(other.msgid)) {
                return false;
            }
            return true;
        }
    }
    
    public final static class Timeout implements Event {
        // 0 means permanent (ban)
        long duration;
        String reason, user;
        
        public Timeout() {
            this.duration = 0;
            this.reason = "";
            this.user = "";
        }
        
        public Timeout(long duration, String reason, String user) {
            this.duration = duration;
            this.reason = reason;
            this.user = user;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public void setDuration(long duration) {
            this.duration = duration;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getUser() {
            return user;
        }
        
        public void setUser(String user) {
            this.user = user;
        }
        
        @Override
        public String toString() {
            return String.format("Timeout [duration=%s, reason=%s, user=%s]", duration, reason, user);
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (duration ^ duration >>> 32);
            result = prime * result + (reason == null ? 0 : reason.hashCode());
            result = prime * result + (user == null ? 0 : user.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Timeout other = (Timeout) obj;
            if (duration != other.duration) {
                return false;
            }
            if (reason == null) {
                if (other.reason != null) {
                    return false;
                }
            } else if (!reason.equals(other.reason)) {
                return false;
            }
            if (user == null) {
                if (other.user != null) {
                    return false;
                }
            } else if (!user.equals(other.user)) {
                return false;
            }
            return true;
        }
        
    }
    
    public final static class Host implements Event {
        String viewers;
        String target;
        
        public Host() {
            this.viewers = "";
            this.target = null;
        }
        
        public Host(String viewers, String target) {
            this.viewers = viewers;
            this.target = target;
        }
        
        public String getViewers() {
            return viewers;
        }
        
        public void setViewers(String viewers) {
            this.viewers = viewers;
        }
        
        public String getTarget() {
            return target;
        }
        
        public void setTarget(String target) {
            this.target = target;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (target == null ? 0 : target.hashCode());
            result = prime * result + (viewers == null ? 0 : viewers.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Host other = (Host) obj;
            if (target == null) {
                if (other.target != null) {
                    return false;
                }
            } else if (!target.equals(other.target)) {
                return false;
            }
            if (viewers == null) {
                if (other.viewers != null) {
                    return false;
                }
            } else if (!viewers.equals(other.viewers)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return String.format("Host [viewers=%s, target=%s]", viewers, target);
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
        
        @Override
        public String toString() {
            return String.format("Leaderboard [usernames=%s, points=%s]", Arrays.toString(usernames),
                    Arrays.toString(points));
        }
    }
    
    // Observable Event types
    public static enum Events {
        MESSAGE,
        WHISPER,
        NOTICE,
        TIMEOUT,
        HOST,
        ROOMSTATE,
        SUBSCRIPTION,
        JOIN,
        PART
    }
    
    // implemented by the model class of every observable event
    public static interface Event {
    };
    
    // implemented by classes that want to observ events
    public static interface EventObserver {
        public void onEvent(Events type, Event e);
    }
    
    // used internally to save registered observers
    public static class EventObserverEntry {
        public EventObserver observer;
        public EnumSet<Events> types;
        
        public EventObserverEntry(EnumSet<Events> types, EventObserver obs) {
            this.observer = obs;
            this.types = types;
        }
    }
    
    // Mark class for automated creation and registration as observer
    @Target(TYPE)
    @Retention(RUNTIME)
    public static @interface Autoload {
    }
    
    // Mark class for priority execution
    @Target(TYPE)
    @Retention(RUNTIME)
    public static @interface CallFirst {
    }
    
    @Target(TYPE)
    @Retention(RUNTIME)
    public static @interface EventTypes {
        Events[] value();
    }
}
