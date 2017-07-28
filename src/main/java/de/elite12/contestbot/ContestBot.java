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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.Events;

public class ContestBot{

	private Properties config;
	private static Logger logger = Logger.getLogger(ContestBot.class);
	private Connection connection;
	private ExecutorService threadPool;
	
	protected static ContestBot instance;
	

	public ContestBot() {
		instance = this;
		
		File propertiesFile = new File("config.properties");
		config = new Properties();
		try (
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(propertiesFile))
		) {
			config.load(bis);
			bis.close();
		} catch (RuntimeException | IOException e) {
			logger.fatal("Can not read config", e);
			System.exit(1);
		}
		
		this.threadPool = Executors.newFixedThreadPool(Integer.parseInt(this.config.getProperty("threads", "2")));
		for(int i = 0; i < Integer.parseInt(this.config.getProperty("threads", "2")); i++) {
			this.threadPool.execute(new MessageParser());
		}
		
		try {
			connection = new Connection();
		} catch (URISyntaxException e) {
			logger.fatal("Can not use parameter ircserver", e);
			System.exit(1);
		}
		
		// Load all modules
		loadModules();
		
		
		connection.connect();
	}
	
	protected ContestBot(boolean testmode) {
		
	}

	protected void loadModules() {
		Reflections reflections = new Reflections("de.elite12.contestbot.modules");
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(Model.Autoload.class);
		for (Class<?> c : classes) {
			try {
				Object i = c.getConstructor().newInstance();
				if (c.isAnnotationPresent(Model.EventTypes.class)) {
					Events[] types = c.getAnnotation(Model.EventTypes.class).value();
					EnumSet<Events> set = EnumSet.noneOf(Events.class);
					set.addAll(Arrays.asList(types));
					MessageParser.registerObserver(set, (EventObserver) i);
				}
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException | ClassCastException e) {
				logger.warn(String.format("Can not autoload class %s", c.getName()), e);
			}
		}
	}
	
	public String getConfig(String key) {
		return config.getProperty(key);
	}
	
	public static ContestBot getInstance() {
		return instance;
	}
	
	public Connection getConnection() {
		return this.connection;
	}

	public void reconnect() {
		logger.debug("Reconnecting");
		if(this.connection.isOpen()) {
			this.connection.close();
		}
		
		try {
			this.connection = new Connection();
		} catch (URISyntaxException e) {
			logger.fatal("This should never happen",e);
			System.exit(1);
		}
		
		boolean success = false;
		while(!success) {
			try {
				success = this.connection.connectBlocking();
			} catch (InterruptedException e) {
				if(logger.isDebugEnabled())
					logger.warn("Error while reconnecting, retrying",e);
				else
					logger.warn("Error while reconnecting, retrying");
			}
		}
		logger.info("Sucessfully reconnected");
	}
}
