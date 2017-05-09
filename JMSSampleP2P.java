package wasdev.sample.jms.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class JMSSampleP2P
 */
@WebServlet("/JMSSampleP2P")
public class JMSSampleP2P extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String EXAMPLE_URL = "Examples : http://localhost:9080/seminar/JMSApp/JMSSampleP2P?ACTION=sendMessage"
											+ "\n         : http://localhost:9080/seminar/JMSApp/JMSSamplePubSub?ACTION=durableSubscriber";
	private static final String QUEUE_CONN_FACTORY = "java:comp/env/jndi_JMS_QUEUE_CF";
	private static final String INPUT_QUEUE = "java:comp/env/jndi_INPUT_Q";
	private static final String REPLY_QUEUE = "java:comp/env/jndi_REPLY_Q";
	private static final String MDB_QUEUE = "java:comp/env/jndi/MDBQ";
	private static final String SEND_RECEIVE = "sendAndReceive";
	private static final String SEND = "sendMessage";
	private static final String RECEIVE_ALL = "receiveAllMessages";
	private static final String RECEIVE_ALL_SELECTORS = "receiveAllMessagesSelectors";
	private static final String MDB_SEND_RECEIVE = "mdbRequestResponse";
	public JMSSampleP2P() {
		super();
	}

/**
 * Metoda obsługująca różne możliwe akcje naszej aplikacji
 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String strAction = request.getParameter("ACTION");
		PrintWriter out = response.getWriter();
		try {
			if (strAction == null) {
				out.println("Please specify the Action");
				out.println(EXAMPLE_URL);
			} else if (strAction.equalsIgnoreCase(SEND_RECEIVE)) {
				sendAndReceive(request, response);
			} else if (strAction.equalsIgnoreCase(SEND)) {
				sendMessage(request, response);
			} else if (strAction.equalsIgnoreCase(RECEIVE_ALL)) {
				receiveAllMessages(request, response);
			} else if (strAction
					.equalsIgnoreCase(RECEIVE_ALL_SELECTORS)) {
				receiveAllMessagesSelectors(request, response);
			} else if (strAction.equalsIgnoreCase(MDB_SEND_RECEIVE)) {
				mdbRequestResponse(request, response);
			} else {
				out.println("Incorrect Action Specified, the valid actions are");
				out.println("ACTION=" + SEND_RECEIVE);
				out.println("ACTION=" + SEND);
				out.println("ACTION=" + RECEIVE_ALL);
				out.println("ACTION=" + RECEIVE_ALL_SELECTORS);
				out.println("ACTION=" + MDB_SEND_RECEIVE);
			}

		} catch (Exception e) {
			out.println("Something unexpected happened, check the logs or restart the server");
			e.printStackTrace();
		}

	}

	/**
	* Scenariusz: Punkt do punktu. Łączy się przy użyciu fabryki połączeń
	* "Jndi_JMS_QUEUE_CF". Wysyła jedną wiadomość do kolejki
	* "Jndi_INPUT_Q"  Odbiera wiadomość i drukuje ją na konsoli
	*/
	public void sendAndReceive(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("SendAndReceive Started");

		/*
		 * InitialContext - klasa do rozwiązywania nazw.
		 * lookup("nazwa") - funkcja pozwalająca znaleźć obiekt o podanej nazwie
		 * comp - components, en - environment
		 * "jndi_JMS_QUEUE_CF", jndi_INPUT_Q - odnosi do konfiguracji w pliku server.xml
		 * Dla kolejki Punkt do punktu - pozwala stworzyć połączenie
		 */
		QueueConnectionFactory connFactory = (QueueConnectionFactory) new InitialContext()
		.lookup(QUEUE_CONN_FACTORY);

		// Utwórz kolejkę, szukając w repozytorium JNDI
		Queue queue = (Queue) new InitialContext().lookup(INPUT_QUEUE);
		
		// Uruchamia połącznie by możliwe było przesyłanie wiadomości
		QueueConnection connection = connFactory.createQueueConnection();
		connection.start();

		// tworzenie sesji w której będą wysyłane wiadomości
		// parametry: boolean - czy odbywa się w transakcji
		// int - czy przyjmujemy każdą wiadomość. Jeśli boolean==true - ignorowane
		// QueueSession pozwala też tworzyć wiadomości, które będą później wysyłane.
		QueueSession sessionSender = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		//stworzenie prducera/sendera który będzie wysyłał dane do podanej jako parametr kolejki 
		QueueSender sender = sessionSender.createSender(queue);
		
		
		// wysłanie wiadomości tekstowej
		sender.send(sessionSender.createTextMessage("Liberty Sample Message"));

		out.println("Message sent successfully");
		
		// tworzymy odbiorcę dla naszej kolejki - korzystając z sesji
		QueueReceiver rec = sessionSender.createReceiver(queue);

		// odbieramy wiadomość która jest w kolejce
		// ponieważ wiadomości mogą mieć różne typy rzutujemy na ten który nas interesuje
		TextMessage msg = (TextMessage) rec.receive();

		out.println("Received Message Successfully :" + msg);

		//jeśli w ogóle istniało połączenie to je zamykamy
		if (connection != null)
			connection.close();
		out.println("SendAndReceive Completed");
	}
	
	/**
	* Scenariusz: Punkt do punktu. Łączy przy użyciu fabryki połączeń
	* "Jndi_JMS_QUEUE_CF" zdefiniowany w pliku server.xml. Wysyła jedną wiadomość do kolejki
	*  "jndi_INPUT_Q" w zdefiniowany serwerze.xml.
	*/

	public void sendMessage(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("SendMessage Started");
		
		QueueConnectionFactory connFactory = (QueueConnectionFactory) new InitialContext()
		.lookup(QUEUE_CONN_FACTORY);
		
		// Utwórz kolejkę, szukając w repozytorium JNDI
		Queue queue = (Queue) new InitialContext().lookup(INPUT_QUEUE);

		// Utwórz połączenie kolejki
		QueueConnection connection = connFactory.createQueueConnection();
		connection.start();

		QueueSession sessionSender = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = sessionSender.createSender(queue);

		TextMessage msg = sessionSender.createTextMessage();
		msg.setStringProperty("COLOR", "BLUE");
		msg.setText("Liberty Sample Message");

		send.send(msg);
		out.println("Message sent successfuly");

		if (connection != null)
			connection.close();
		out.println("SendMessage Completed");
	}

	/**
	* Scenariusz: Punkt do punktu. Łączy się  przy użyciu fabryki połączeń
	* "Jndi_JMS_QUEUE_CF" zdefiniowanej w pliku server.xml. Wysyła jedną wiadomość do 
	kolejki określona w jndi_INPUT_Q zdefiniowanej w pliku server.xml.
	Otrzymuje wszystkie wiadomości z powyższej kolejki i drukuje je na konsole.
	*/

	public void receiveAllMessages(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("ReceiveAllMessages Started");

		QueueConnectionFactory connFactory = (QueueConnectionFactory) new InitialContext()
		.lookup(QUEUE_CONN_FACTORY);

		// Utwórz kolejkę, szukając w repozytorium JNDI
		Queue queue = (Queue) new InitialContext().lookup(INPUT_QUEUE);

		// Utwórz połączenie kolejki
		QueueConnection connection = connFactory.createQueueConnection();
		connection.start();

		QueueSession session = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		QueueReceiver receive = session.createReceiver(queue);

		TextMessage msg = null;

		do {
			// 2000 - timeout -> jeśli przez 2000 milisekund nic nie przyjdzie zwróci null
			// dla wartości 0 czeka na wiadomość dopóki jakakolwiek nie przyjdzie
			msg = (TextMessage) receive.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);

		if (connection != null)
			connection.close();

		out.println("ReceiveAllMessages Completed");

	} 

	/**
	* Scenariusz: Punkt do punktu. Łączy przy użyciu fabryki połączeń
	* "Jndi_JMS_QUEUE_CF". Wysyła jedną wiadomość do kolejki "jndi_INPUT_Q".
	* Odbiera wiadomości z selektorem COLOR = 'BLUE' i drukuje ją na konsoli.
	*/

	public void receiveAllMessagesSelectors(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("ReceiveAllMessagesSelectors Started");

		QueueConnectionFactory connFactory = (QueueConnectionFactory) new InitialContext()
		.lookup(QUEUE_CONN_FACTORY);
		
		// Utwórz kolejkę, szukając w repozytorium JNDI
		Queue queue = (Queue) new InitialContext().lookup(INPUT_QUEUE);

		// Utwórz połączenie kolejki
		QueueConnection connection = connFactory.createQueueConnection();
		connection.start();

		QueueSession session = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);
		
		// selector (drugi parametr) powoduje, że odebrane zostaną tylko takie wiadomości które posiadają daną właściwość
		// brak selectora lub pusty string oznaczają że wszystkie wiadomości zostaną odebrane
		QueueReceiver receive = session.createReceiver(queue, "COLOR='BLUE'");
		TextMessage msg = null;

		do {
			msg = (TextMessage) receive.receive(2000);
			if(msg!=null)
			out.println("Received  messages " + msg);
		} while (msg != null);

		if (connection != null)
			connection.close();

		out.println("ReceiveAllMessagesSelectors Completed");

	}

	/**
	* Scenariusz: punkt do punktu, działa w połączeniu z MDB. 
	* Wyślij wiadomość do kolejki MDB_QUEUE. 
	* SampleMDB odbierze wiadomość i wyśle tę otrzymaną wiadomość do MDBREPLYQ. 
	* Ta wiadomość zostanie następnie odebrana z kolejki MDBREPLYQ.
	 */
	public void mdbRequestResponse(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		PrintWriter out = response.getWriter();
		out.println("MDBRequestResponse Started");

		// Wyślij wiadomość do MDB_QUEUE
		QueueConnectionFactory connFactory = (QueueConnectionFactory) new InitialContext()
		.lookup(QUEUE_CONN_FACTORY);
		// za kolejkę odpowiada (odbiera z niej wiadomości) MDB - klasa SampleMDB
		// ustawione w pliku konfiguracyjnym server.xml
		Queue queue = (Queue) new InitialContext().lookup(MDB_QUEUE);
		QueueConnection connection = connFactory.createQueueConnection();
		connection.start();
		QueueSession session = connection.createQueueSession(false,
				javax.jms.Session.AUTO_ACKNOWLEDGE);

		QueueSender send = session.createSender(queue);

		TextMessage msg = session.createTextMessage();
		
		// Dodawanie właściwości do wiadomości
		msg.setStringProperty("COLOR", "BLUE");
		msg.setText("MDB Test - Message to MDB");
		send.send(msg);

		out.println("Message sent successfully");

		// Message Driven Bean (MDB)
		// czekamy aż wiadomość dojdzie do MDB, a następnie my dostaniemy odpowiedź
		// tu tworzymy drugą kolejkę, która będzie odbierać to co SampleMDB nam odpowie.
		Queue queue2 = (Queue) new InitialContext().lookup(REPLY_QUEUE);
		QueueReceiver receiver = session.createReceiver(queue2);

		boolean messageReceived = false;
		do {
			// czekamy 5 sekund na odpowiedź
			msg = (TextMessage) receiver.receive(5000);
			if (msg != null) {
				// jeśli coś przyszło, to znaczy, że SampleMDB dostało wiadomość i odesłało odpowiedź.
				messageReceived = true;
				out.println("Received messages from MDBREPLYQ" + msg);
			}
		} while (msg != null);

		if (!messageReceived) {
			throw new Exception("MDB did not receive the Message");
		} else {
			out.println("MDB has successfully received the message");
		}

		if (connection != null)
			connection.close();
		out.println("MDBRequestResponse Completed");

	}
}