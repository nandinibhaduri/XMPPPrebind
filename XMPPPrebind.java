import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XMPPPrebind {

	protected String sid;
	protected String rid;
	protected String jid;
	protected String boshUrl;
	protected String user;
	protected String domain;
	protected String password;

	public XMPPPrebind(String boshUrl, String domain, String user, String password) {
		this.boshUrl = boshUrl;
		this.user = user;
		this.domain = domain;
		this.password = password;
	}

	public String getSid() {
		return sid;
	}

	public String getRid() {
		return rid;
	}

	public String getJid() {
		return jid;
	}

	public void connect() throws Exception {
		String hash = Base64.getEncoder()
				.encodeToString((this.user + "@" + this.domain + "\0" + this.user + "\0" + this.password).getBytes());

		int rid = new Random().nextInt(32768);

		String jid = this.user + "@" + this.domain;

		String body = "<body rid='" + rid + "' " + "xmlns='http://jabber.org/protocol/httpbind' " + "to='" + this.domain
				+ "' xml:lang='en' wait='60' hold='1' " + "window='5' content='text/xml; charset=utf-8' "
				+ "ver='1.6' xmpp:version='1.0' xmlns:xmpp='urn:xmpp:xbosh'/>";

		String output = this.__sendBody(body);
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(output));
		Document xml = builder.parse(is);

		String sid = xml.getDocumentElement().getAttribute("sid");

		rid++;
		body = "<body rid='" + rid + "' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid + "'>"
				+ "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>" + hash + "</auth>" + "</body>";

		this.__sendBody(body);

		rid++;
		body = "<body rid='" + rid + "' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid + "' to='"
				+ this.domain + "' xml:lang='en' xmpp:restart='true' xmlns:xmpp='urn:xmpp:xbosh'/>";
		this.__sendBody(body);

		rid++;
		body = "<body rid='" + rid + "' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid + "'>"
				+ "<iq type='set' id='_bind_auth_2' xmlns='jabber:client'>"
				+ "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/>" + "</iq>" + "</body>";

		this.__sendBody(body);
		rid++;
		body = "<body rid='" + rid + "' xmlns='http://jabber.org/protocol/httpbind' sid='" + sid + "'>"
				+ "<iq type='set' id='_session_auth_2' xmlns='jabber:client'>"
				+ "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>" + "</iq>" + "</body>";
		this.__sendBody(body);
		rid++;

		body = "<body rid='" + rid + "' sid='" + sid + "' xmlns='http://jabber.org/protocol/httpbind'>"
				+ "<iq id='bind_1' type='set' xmlns='jabber:client'>"
				+ "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>" + "<resource>httpclient</resource>" + "</bind>"
				+ "</iq>" + "</body>";
		this.__sendBody(body);
		rid++;

		this.rid = rid+"";
		this.sid = sid;
		this.jid = jid;

	}

	private String __sendBody(String body) throws Exception {

		URL url = new URL(this.boshUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setConnectTimeout(100000);
        conn.setReadTimeout(100000);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		OutputStream os = conn.getOutputStream();
		os.write(body.getBytes());
		os.flush();

		int responseCode = conn.getResponseCode();

		if (responseCode != 200) {
			throw new Exception(conn.getResponseMessage());
		}

		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		String line;
		StringBuilder result = new StringBuilder();
		while ((line = br.readLine()) != null) {
			result.append(line);
		}

		return result.toString();

	}

	public static void main(String[] args) {

		XMPPPrebind xmpp = new XMPPPrebind("http://localhost:5280/http-bind/", "domain", "uname", "password");

		try {
			xmpp.connect();
			
			System.out.println(xmpp.rid);
			System.out.println(xmpp.sid);
			System.out.println(xmpp.jid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
