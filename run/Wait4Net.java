import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic utility to wait for open port or HTTP status code.
 */
public class Wait4Net {
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_BLUE = "\u001B[34m";

	public static void main(String[] args) {
		Wait4Net wait4Net =new Wait4Net();
		long started = System.currentTimeMillis();
		if(args.length!=3) {
			wait4Net.log("expected 3 arguments:" +
				"wait for a network response:"+
				"\nwait4HttpStatus: url status-code[,status-code]* timeoutMs)"+
				"\nwait4Port: hostname port timeoutMs");
		}
		long timeoutMillis = Long.parseLong(args[2]);
		boolean success;
		if(args[0].matches("^https*://.*")) {
			List<Integer> statusCodes = Stream.of(args[1].split(","))
				.map(Integer::parseInt)
				.collect(Collectors.toList());
			success=wait4Net.wait4HttpStatus(args[0], statusCodes, timeoutMillis);
		} else {
			success=wait4Net.wait4Port(args[0], Integer.parseInt(args[1]), timeoutMillis);
		}
		System.exit(success?0:1);
	}

	public boolean wait4Port(String hostname, int port, long timeoutMs) {
		String actionMessage = String.format("waiting for %s:%d", hostname, port);
		logInfo(actionMessage);
		long startTs = System.currentTimeMillis();
		boolean scanning = true;
		String message = "";
		while (scanning) {
			if (System.currentTimeMillis() - startTs > timeoutMs) {
				logError(String.format("timeout %s",actionMessage));
				return false;
			}
			try {
				SocketAddress address = new InetSocketAddress(hostname, port);
				Selector.open();
				try (SocketChannel socketChannel = SocketChannel.open()) {
					socketChannel.configureBlocking(true);
					socketChannel.connect(address);
				}

				scanning = false;
			} catch (Exception e) {
				String newMessage = String.format("%s: %s", actionMessage, e.getMessage());
				if(!message.equals(newMessage)) {
					logWarn(newMessage);
					message = newMessage;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException ie) {
					logError(String.format("interrupted %s",actionMessage));
					return false;
				}
			}
		}
		logInfo(String.format("success %s.", actionMessage));
		return true;
	}

	public boolean wait4HttpStatus(String url, Collection<Integer> expectedStatuses, long timeoutMS) {
		HttpClient client = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.ALWAYS)
			.connectTimeout(Duration.ofSeconds(2))
			.sslContext(trustingSSLContext()).build();
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(4))
			.build();
		String actionMessage = String.format("waiting %d ms for %s to respond with status: %s",
				timeoutMS, url, ""+expectedStatuses);
		logInfo(actionMessage);
		long startTS = System.currentTimeMillis();
		int lastStatus = 0;
		int attempts = 0;
		String message = "";
		while (System.currentTimeMillis() - startTS < timeoutMS) {
			try {
				attempts++;
				HttpResponse<String> response = client.send(request,
					HttpResponse.BodyHandlers.ofString());
				lastStatus = response.statusCode();
				if(expectedStatuses.contains(lastStatus)) {
					logInfo(String.format("success %s, last status=%d, %d attempts",
						actionMessage, lastStatus, attempts));
					return true;
				}
			} catch (Exception e) {
				String newMessage = String.format("warning: %s", actionMessage);
				if(!message.equals(newMessage)) {
					logWarn(newMessage);
					message = newMessage;
				}
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logError(String.format("interrupted %s",actionMessage));
				return false;
			}
		}

		logError(String.format("timeout %s, last status=%d, %d attempts",
			actionMessage, lastStatus, attempts));
		return false;
	}

	private SSLContext trustingSSLContext() {
		TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() { return null; }

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			}
		};

		System.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			return sslContext;
		} catch (NoSuchAlgorithmException|KeyManagementException e) {
			log(e.getMessage());
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	private void logError(String message) {
		log(ANSI_RED+message+ANSI_RESET);
	}
	private void logWarn(String message) {
		log(ANSI_YELLOW+message+ANSI_RESET);
	}
	private void logInfo(String message) {
		log(ANSI_GREEN+message+ANSI_RESET);
	}
	private void logError(String message, Throwable t) {
		log(ANSI_RED+message+ANSI_RESET, t);
	}
	private void logWarn(String message, Throwable t) {
		log(ANSI_YELLOW+message+ANSI_RESET, t);
	}
	private void logInfo(String message, Throwable t) {
		log(ANSI_GREEN+message+ANSI_RESET, t);
	}
	private void log(String message) { log(message, null); }
	private void log(String message, Throwable t) {
		String ts=ZonedDateTime.now( /*ZoneOffset.UTC */).format(DateTimeFormatter.ISO_INSTANT );
		ts=ts.substring(0,23).replace("T", "");
		ts = ANSI_BLUE + ts + ANSI_RESET;
		System.out.println(String.format("%s: %s - %s", ts, getClass().getSimpleName(), message));
		if (t != null) {
			t.printStackTrace();
		}
	}
}
