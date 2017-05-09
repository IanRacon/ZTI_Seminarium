package wasdev.sample.jms.mdb;

import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;

@MessageDriven
public class SampleMDB implements MessageListener {
	//zasoby z pliku konfiguracyjnego server.xml
	private static final String QUEUE_CONN_FACTORY = "java:comp/env/jndi_JMS_QUEUE_CF";
	private static final String REPLY_QUEUE = "java:comp/env/jndi_REPLY_Q";
	
	//metoda z interfejsu MessageListener - do przechwytywania przysłanej wiadomości
	@Override
	public void onMessage(Message message) {
		try {
			SendMDBResponse(message);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void SendMDBResponse(Message msg) throws Exception {
		QueueConnectionFactory connFactory = 
				(QueueConnectionFactory) new InitialContext().lookup(QUEUE_CONN_FACTORY);
		
		Queue replyQueue = (Queue) new InitialContext().lookup(REPLY_QUEUE);
		
		QueueConnection connection = connFactory.createQueueConnection();
		
		connection.start();
	
		QueueSession queueSession = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender sender = queueSession.createSender(replyQueue);
	
		sender.send(msg);
	
		if (connection != null)
			connection.close();
	}
}