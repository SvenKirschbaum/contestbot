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

import static org.junit.Assert.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.modules.TestModule;

public class ContestBotTest {

	private static ContestBot c;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ContestBotTest.c = new ContestBot(true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ContestBotTest.c = null;
	}
	
	@Before
	public void setUpBeforeTest() throws Exception {
		TestModule.calls.clear();
	}

	@Test
	public void testLoadModules() {
		Logger.getLogger(ContestBot.class).setLevel(Level.ERROR);
		c.loadModules();
		assertNotNull("TestModule has not been loaded", TestModule.instance);
		
		MessageParser.notifyObservers(Events.HOST, new Model.Host("5","1"));
		MessageParser.notifyObservers(Events.WHISPER, new Model.Message("fallobst22","hi",null));
		MessageParser.notifyObservers(Events.MESSAGE, new Model.Message("fallobst22","hallo",null));
		MessageParser.notifyObservers(Events.TIMEOUT, new Model.Timeout(0,"blub","fallobst22"));
		MessageParser.notifyObservers(Events.WHISPER, new Model.Message("fallobst22","blub",null));
		MessageParser.notifyObservers(Events.NOTICE, new Model.Notice("abc123","fallobst22","hallo"));
		MessageParser.notifyObservers(Events.SUBSCRIPTION, new Model.Message("fallobst22","",null));
		
		assertEquals("Unexspected Event", new Model.Message("fallobst22","hi",null), TestModule.calls.remove());
		assertEquals("Unexspected Event", new Model.Timeout(0,"blub","fallobst22"), TestModule.calls.remove());
		assertEquals("Unexspected Event", new Model.Message("fallobst22","blub",null), TestModule.calls.remove());
		assertNull("Unexspected Event", TestModule.calls.poll());
	}

}
