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
	public void connect() {
		super.connect();
	}


	@Override
	public void onClose(int code, String reason, boolean remote) {
		Logger.getLogger(Connection.class).warn("Lost connection. Code ["+code+"] Reason ["+reason+"] Remote["+remote+"]");
		//TODO: reconnect
		System.exit(1);
	}

	@Override
	public void onError(Exception ex) {
		Logger.getLogger(Connection.class).error(ex);
	}

	@Override
	public void onMessage(String message) {
		String[] messages = message.split("\r\n");
		for(String s:messages) {
			try {
				if(!s.isEmpty())
					ContestBot.getInstance().getParser().queueElement(s);
			}
			catch(BufferOverflowException e) {
				Logger.getLogger(Connection.class).warn("Dropped Message: "+s,e);
			}
		}
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		this.send("PASS " + ContestBot.getInstance().getConfig("oauth"));
        this.send("NICK " + ContestBot.getInstance().getConfig("login"));
	}
	
	public void sendChatMessage (String message) {
		this.send("PRIVMSG #" + ContestBot.getInstance().getConfig("channelname") + " :"+message);
	}
	
	public void sendPrivatMessage (String user, String message) {
		this.send("PRIVMSG #" + ContestBot.getInstance().getConfig("channelname") + " :/w "+user+" "+message);
	}
}
