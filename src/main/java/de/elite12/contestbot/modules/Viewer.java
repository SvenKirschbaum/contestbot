package de.elite12.contestbot.modules;

import org.apache.log4j.Logger;

import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Message;

@Autoload
@EventTypes({ Events.JOIN, Events.PART })
public class Viewer implements EventObserver {
    
    public Viewer() {

    }

    @Override
    public void onEvent(Events type, Event e) {
        Message m = (Message) e;
        switch (type) {
            case JOIN: {
                Logger.getLogger(Viewer.class).info("JOIN: " + m.getUsername());
                break;
            }
            case PART: {
                Logger.getLogger(Viewer.class).info("LEAVE: " + m.getUsername());
                break;
            }
        }
    }

}
