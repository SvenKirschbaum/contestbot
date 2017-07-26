package de.elite12.contestbot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

public class ContestBot{

	public Properties config;
	private static Logger logger = Logger.getLogger(ContestBot.class);
	private Connection connection;
	private MessageParser parser;
	private Contest contest;
	
	public static ContestBot instance;
	

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
		this.parser.start();
		
		this.contest = new Contest();
		
		try {
			connection = new Connection();
		} catch (URISyntaxException e) {
			logger.fatal("Can not use parameter ircserver", e);
			System.exit(1);
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
	
	public Contest getContest() {
		return this.contest;
	}
}
