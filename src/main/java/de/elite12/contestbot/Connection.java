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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferOverflowException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

public class Connection extends WebSocketClient {

    public Connection() throws URISyntaxException {
        super(new URI(ContestBot.getInstance().getConfig("ircserver")), new Draft_6455(), null, 5000);
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            this.setSocket(sslSocketFactory.createSocket());
            this.setTcpNoDelay(true);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Logger.getLogger(Connection.class).fatal("Can not build Socket", e);
            System.exit(1);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logger.getLogger(Connection.class)
                .warn(String.format("Lost connection. Code [%d] Reason [%s] Remote[%b]", code, reason, remote));

        ContestBot.getInstance().reconnect();
    }

    @Override
    public void onError(Exception ex) {
        Logger.getLogger(Connection.class).error(ex);
    }

    @Override
    public void onMessage(String message) {
        String[] messages = message.split("\r\n");
        for (String s : messages) {
            try {
                if (!s.isEmpty()) {
                    MessageParser.queueElement(s);
                }
            } catch (BufferOverflowException e) {
                Logger.getLogger(Connection.class).warn(String.format("Dropped Message: %s", s), e);
            }
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        this.send(String.format("PASS %s", ContestBot.getInstance().getConfig("oauth")));
        this.send(String.format("NICK %s", ContestBot.getInstance().getConfig("login")));
    }

    public void sendChatMessage(String message) {
        this.send(String.format("PRIVMSG #%s :%s", ContestBot.getInstance().getConfig("channelname"), message));
    }

    public void sendPrivatMessage(String user, String message) {
        this.send(String.format("PRIVMSG #%s :/w %s %s", ContestBot.getInstance().getConfig("channelname"), user,
                message));
    }

    public void sendMessage(boolean whisper, String user, String message) {
        if (whisper) {
            sendPrivatMessage(user, message);
        } else {
            sendChatMessage(message);
        }
    }
}
