package de.elite12.contestbot.modules;

import de.elite12.contestbot.AuthProvider;
import de.elite12.contestbot.ContestBot;
import de.elite12.contestbot.Model.Autoload;
import de.elite12.contestbot.Model.Event;
import de.elite12.contestbot.Model.EventObserver;
import de.elite12.contestbot.Model.EventTypes;
import de.elite12.contestbot.Model.Events;
import de.elite12.contestbot.Model.Message;

@Autoload
@EventTypes({ Events.MESSAGE })
public class AuthHook implements EventObserver {
    @Override
    public void onEvent(Events type, Event e) {
        Message m = (Message) e;
        if (m.getTags() != null) {
            if (m.getTags().containsKey("mod")) {
                if (m.getTags().get("mod").equals("1")) {
                    AuthProvider.MarkPrivileged(m.getUsername());
                }
            }
        }
        if (m.getUsername().equalsIgnoreCase(ContestBot.getInstance().getConfig("channelname"))) {
            AuthProvider.MarkPrivileged(m.getUsername());
        }
    }
}