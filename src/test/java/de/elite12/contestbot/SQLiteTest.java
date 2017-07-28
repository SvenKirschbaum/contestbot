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

import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.elite12.contestbot.Model.Leaderboard;

public class SQLiteTest {
	
	private static SQLite sql;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		sql = new SQLite("jdbc:sqlite::memory:");
	}
	
	@AfterClass
	public static void cleanupAfterClass() throws Exception {
		sql.con.close();
	}

	@Test
	public void testPoints() throws SQLException {
		assertEquals("Wrong point value", 0, sql.getPoints("fallobst22"));
		sql.addPoints("fallobst22", 5);
		sql.addPoints("tester", 7);
		assertEquals("Wrong point value", 5, sql.getPoints("fallobst22"));
		assertEquals("Wrong point value", 7, sql.getPoints("tester"));
		sql.addPoints("fallobst22", 15);
		sql.addPoints("blub", 14);
		assertEquals("Wrong point value", 20, sql.getPoints("fallobst22"));
		assertEquals("Wrong point value", 14, sql.getPoints("blub"));
		
		Leaderboard l = sql.getLeaderboard(3);
		assertArrayEquals("Wrong leaderboard", l.getUsernames(), new String[] {"fallobst22","blub","tester"});
		assertArrayEquals("Wrong leaderboard", l.getPoints(), new Integer[] {20,14,7});
	}

}
