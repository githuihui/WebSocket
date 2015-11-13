import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@ServerEndpoint(value = "/websocket/chat")
public class WebSocket {

	private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static final Map<String,WebSocket> connections = new ConcurrentHashMap<String,WebSocket>();  
	private Session session;
	private String  userUUID;

	@OnOpen
	public void start(Session session) {
		this.session = session;
		 userUUID = UUID.randomUUID().toString()+"-"+(int) (Math.random() * 100000);
		 connections.put(userUUID,this);
		 try {
			this.session.getBasicRemote().sendText("HEAD:UUID:"+userUUID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnClose
	public void end() {
		connections.remove(this);
	}

	@OnMessage
	public void incoming(String message) {
		log.info("message:{}", message);
		String headSuccess = "HEAD:SUCCESS:";
		if (message.indexOf(headSuccess) > -1) {
			String user = message.substring(headSuccess.length(), message.indexOf(":OPENID:"));
			sendMessage(user, message);
		}
	}

	@OnError
	public void onError(Throwable t) throws Throwable {
		// log.error("Chat Error: " + t.toString(), t);
	}
	
	public static void sendMessage(String user,String msg) {
		WebSocket client = connections.get(user);
		if(client ==null) return;
			try {
				client.session.getBasicRemote().sendText(msg);
			} catch (IOException e) {
				connections.remove(client);
			try {
					client.session.close();
				} catch (IOException e1) {
				}

			}
	}
		
	public static void broadcast(String msg) {
		 Set<String> keySet = connections.keySet();  
         for (String key : keySet) {  
        	 WebSocket client = connections.get(key);
			try {
				synchronized (client) {
					client.session.getBasicRemote().sendText(msg);
				}
			} catch (IOException e) {
				connections.remove(client);
				try {
					client.session.close();
				} catch (IOException e1) {
				}

			}
		}
	}

}
