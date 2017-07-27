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

import org.apache.log4j.Logger;
import org.reflections.Reflections;

import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.Events;

public class ContestBot{

	private Properties config;
	private static Logger logger = Logger.getLogger(ContestBot.class);
	private Connection connection;
	private MessageParser parser;
	
	private static ContestBot instance;
	

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
		
		this.parser = new MessageParser();
		new Thread(this.parser).start();
		
		try {
			connection = new Connection();
		} catch (URISyntaxException e) {
			logger.fatal("Can not use parameter ircserver", e);
			System.exit(1);
		}
		
		// Load all modules
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
				logger.warn("Can not autoload class " + c.getName(), e);
			}
		}
		
		
		connection.connect();
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
	
	public MessageParser getParser() {
		return this.parser;
	}
}
