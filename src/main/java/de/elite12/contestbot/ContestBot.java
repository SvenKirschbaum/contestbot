package de.elite12.contestbot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ContestBot extends Thread {

	private Properties config;
	private static Logger logger = Logger.getLogger(ContestBot.class);

	public ContestBot() {
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
	}

	@Override
	public void run() {

	}
}
