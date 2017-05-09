
package wasdev.sample.jms.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/JMSSamplePubSub")
public class JMSSamplePubSub extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String EXAMPLE_URL = "Examples : http://localhost:9080/seminar/JMSApp/JMSSampleP2P?ACTION=sendMessage"
			+ "\n         : http://localhost:9080/seminar/JMSApp/JMSSamplePubSub?ACTION=durableSubscriber";
	private static final String TOPIC_CONN_FACTORY = "java:comp/env/jmsTCF";
	private static final String JMS_TOPIC = "java:comp/env/jmsTopic";
	private static final String DURABLE_SUBSCRIBER = "durableSubscriber";
	private static final String NON_DURABLE_SUBSCRIBER = "nonDurableSubscriber";
	private static final String PUBLISH_MESSAGES = "publishMessages";
	private static final String UNSUB_DURABLE_SUBSCRIBER = "unsubscribeDurableSubscriber";
	
	public JMSSamplePubSub() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String strAction = request.getParameter("ACTION");
		PrintWriter out = response.getWriter();
		try{
			if(strAction == null){
				out.println("Please specify the Action");
				out.println(EXAMPLE_URL);
			}else if(strAction.equalsIgnoreCase(NON_DURABLE_SUBSCRIBER)){
				// Utwórz nietrwałego subskrybenta, opublikuj i otrzymuj wiadomość z tematu
				nonDurableSubscriber(request, response);
			}else if(strAction.equalsIgnoreCase(DURABLE_SUBSCRIBER)){
				// Stwórz trwałego subskrybenta, opublikuj i otrzymuj wiadomość z tematu
				durableSubscriber(request, response);
			}else if(strAction.equalsIgnoreCase(PUBLISH_MESSAGES)){
				// Opublikuj 5 wiadomości do tematu
				publishMessages(request, response);
			}else if(strAction.equalsIgnoreCase(UNSUB_DURABLE_SUBSCRIBER)){
				// Wyrejestruj zarejestrowanego trwałego abonenta 
				unsubscribeDurableSubscriber(request, response);
			}else{
				out.println("Incorrect Action Specified, the valid actions are");
				out.println("ACTION="+NON_DURABLE_SUBSCRIBER);
				out.println("ACTION="+DURABLE_SUBSCRIBER);
				out.println("ACTION="+PUBLISH_MESSAGES);
				out.println("ACTION="+UNSUB_DURABLE_SUBSCRIBER);
			}
		}catch(Exception e){
			out.println("Something unexpected happened, check the logs or restart the server");
			e.printStackTrace();
		}
	}

	/**
	 * Scenariusz: wykonuje nietrwały  pub / sub
	 * Łączy się przy użyciu fabryki połączeń jmsTCF 
	 * Tworzy nietrwałego subskrybenta w temacie jmsTopic 
	 * Opublikuje pojedynczą wiadomość w temacie jmsTopic
	 * Subskrybent otrzymuje wiadomość z tematu jmsTopic, która jest drukowana na konsoli
	 */
	public void nonDurableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("NonDurableSubscriber Started");

		// Stórz fabrykę dla tematów
		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup(TOPIC_CONN_FACTORY);
		// stórz połączenie dla tematu
		TopicConnection connection = cf1.createTopicConnection();
		
		//połącz
		connection.start();
		//Stwórz sesję tla tematu
		TopicSession session = connection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		// Szukaj tematu w JNDI
		Topic topic = (Topic) new InitialContext().lookup(JMS_TOPIC);

		// służy do odbierania wiadomości wrzuconych do tematu
		TopicSubscriber sub = session.createSubscriber(topic);

		//służy do przesyłania wiadomości do tematu
		TopicPublisher publisher = session.createPublisher(topic);

		//prześlij wiadomość do tematu
		publisher.publish(session.createTextMessage("Liberty PubSub Message"));

		//odbierz wiadomość z tematu
		TextMessage msg = (TextMessage) sub.receive(2000);
		if (null == msg) {
			throw new Exception("No message received");
		}else {
			out.println("Received message for non-durable subscriber " + msg);
		}
		if (sub != null)
			sub.close();
		if (connection != null)
			connection.close();

		out.println("NonDurableSubscriber Completed");

	}

	/**
	 * Scenariusz testowy: Przeprowadza trwały pub / sub
	 * Łączy się przy użyciu fabryki połączeń jmsTCF 
	 * Tworzy trwałego subskrybenta w temacie jmsTopic 
	 * Publikuje pojedynczą wiadomość w temacie jmsTopic
	 * Subskrybent otrzymuje wiadomość z tematu jmsTopic
	 */
	public void durableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("DurableSubscriber Started");
		
		// Utwórz fabrykę połączeń tematów
		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup(TOPIC_CONN_FACTORY);
		
		// Szukaj tematu w JNDI
		Topic topic = (Topic) new InitialContext().lookup(JMS_TOPIC);

		// Stwórz sesję tla tematu
		TopicConnection connection = cf1.createTopicConnection();
		connection.start();
		TopicSession session = connection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		// Pozwala odebrać też te wiadomości które zostały wysłane do tematu 
		// w czasie gdy subscriber był wyłączony
		TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");

		//Utwórz publishera
		//służy do przesyłania wiadomości do tematu
		TopicPublisher publisher = session.createPublisher(topic);

		// Opublikuj wiadomość
		publisher.publish(session.createTextMessage("Liberty PubSub Message"));

		TextMessage msg = null;
		do {
			msg = (TextMessage) sub.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);

		if (sub != null)
			sub.close();
		if (connection != null)
			connection.close();
		
		out.println("DurableSubscriber Completed");
	}

	/**
	 * Scenariusz testowy: publikowanie wiadomości do tematu
	 * Łączy się przy użyciu fabryki połączeń jmsTCF 
	 * Publikuje 5 wiadomości do tematu jmsTopic
	 */
	public void publishMessages(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("PublishMessage Started");

		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup(TOPIC_CONN_FACTORY);
		TopicConnection connection = cf1.createTopicConnection();
		int msgs = 5;

		TopicSession session = connection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		Topic topic = (Topic) new InitialContext().lookup(JMS_TOPIC);

		TopicPublisher publisher = session.createPublisher(topic);
		// Wyślij 5 wiadomości
		for (int i = 0; i < msgs; i++) {
			publisher.publish(session.createTextMessage("Liberty PubSub Message : " + i));
		}
		if (connection != null)
			connection.close();
		out.println(msgs+ "Messages published");
		out.println("PublishMessage Completed");
	}


	/**
	 * Scenariusz testowy: wyrejestrowanie trwałego subskrybenta
	 * Łączy się przy użyciu fabryki połączeń jmsTCF 
	 * Tworzy / otwiera trwałego subskrybenta (o nazwie DURATEST) w temacie jmsTopic 
	 * Konsumuje wszystkie wiadomości do tematu jmsTopic 
	 * Subskrybent rezygnuje z subskrypcji tematu jmsTopic
	 */
	public void unsubscribeDurableSubscriber(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PrintWriter out = response.getWriter();
		out.println("UnsubscribeDurableSubscriber Started");

		TopicConnectionFactory cf1 = (TopicConnectionFactory) new InitialContext().lookup(TOPIC_CONN_FACTORY);
		TopicConnection connection = cf1.createTopicConnection();

		connection.start();
		TopicSession session = connection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);

		Topic topic = (Topic) new InitialContext().lookup(JMS_TOPIC);

		TopicSubscriber sub = session.createDurableSubscriber(topic, "DURATEST");
		// Skonsumuj wszystkie istniejące wiadomości dla trwałego abonenta DURATEST
		TextMessage msg = null;
		do {
			msg = (TextMessage) sub.receive(2000);
			if(msg!=null)
				out.println("Received  messages " + msg);
		} while (msg != null);

		if (sub != null)
			sub.close();

		// Porzuć subskrypcje dla tej nazwy subscribera w sesji
		session.unsubscribe("DURATEST");

		if (connection != null)
			connection.close();

		out.println("UnsubscribeDurableSubscriber Completed");
	}

}