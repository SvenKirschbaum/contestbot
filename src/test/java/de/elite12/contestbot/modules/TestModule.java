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
package de.elite12.contestbot.modules;

import java.util.ArrayDeque;
import java.util.Queue;

import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;

@Autoload
@EventTypes({Events.WHISPER,Events.TIMEOUT})
public class TestModule implements EventObserver{
	public static TestModule instance = null;
	public static Queue<Event> calls = new ArrayDeque<>();
	
	public TestModule() {
		TestModule.instance = this;
	}

	@Override
	public void onEvent(Events type, Event e) {
		calls.add(e);
	}
}
