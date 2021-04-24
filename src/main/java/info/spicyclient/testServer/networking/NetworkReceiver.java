package info.spicyclient.testServer.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkReceiver {
	
	private ServerSocket socket;
	
	public void openConnection(int port) throws Exception {
		
		socket = new ServerSocket(port);
		
		new Thread("Server Socket (" + port + ")") {
			public void run() {
				while (true) {
					try {
						final NetworkConnection net = new NetworkConnection();
						net.subscribeToInput(new NetworkSubscription() {
							public void onEvent(String input) {
								System.out.println(" ");
								System.out.println(input);
							}
						});
						net.openConnection(socket.accept());
						System.out.println(net.getAddressAndPort() + " connected");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	public void closeConnection() throws Exception {
		
		if (socket == null) {
			throw new NetworkException();
		}
		
		socket.close();
		socket = null;
	}
}
