package info.spicyclient.testServer.networking;

import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;

import info.spicyclient.testServer.networking.packets.BasicPacket;
import info.spicyclient.testServer.networking.packets.HandshakePacket;

public class NetworkConnection {
	
	private Socket socket;
	private PrintWriter socketServerbound;
	private Scanner socketClientBound;
	private Thread keepAlive, inputStreamThread;
	private CopyOnWriteArrayList<NetworkSubscription> subs = new CopyOnWriteArrayList<NetworkSubscription>();
	private boolean keepThreadsAlive = false;
	private long lastPing = System.currentTimeMillis();
	private String aesKey = Security.getRandomString(32);
	private boolean handshakeCompleted = false;
	
	public void openConnection(String address, int port) throws Exception {
		
		lastPing = System.currentTimeMillis();
		handshakeCompleted = false;
		keepThreadsAlive = true;
		socket = new Socket(address, port);
		socketServerbound = new PrintWriter(socket.getOutputStream());
		socketClientBound = new Scanner(socket.getInputStream());
		keepAlive = createKeepAliveThread();
		inputStreamThread = createInputStreamThread();
		keepAlive.start();
		inputStreamThread.start();
		
	}
	
	public void openConnection(Socket s) throws Exception {
		
		lastPing = System.currentTimeMillis();
		handshakeCompleted = false;
		keepThreadsAlive = true;
		socket = s;
		socketServerbound = new PrintWriter(socket.getOutputStream());
		socketClientBound = new Scanner(socket.getInputStream());
		keepAlive = createKeepAliveThread();
		inputStreamThread = createInputStreamThread();
		keepAlive.start();
		inputStreamThread.start();
		
		sendPacket(new HandshakePacket(Security.rsaKeys.getPublic().getEncoded(), 0));
		
	}
	
	public void closeConnection() throws Exception {
		
		if (socket == null || keepAlive == null) {
			throw new NetworkException();
		}
		
		System.out.println(getAddressAndPort() + " disconnected");
		
		keepThreadsAlive = false;
		
		subs.clear();
		keepAlive = null;
		inputStreamThread = null;
		socketServerbound.close();
		socketClientBound.close();
		socket.close();
		socket = null;
		socketServerbound = null;
		socketClientBound = null;
		
	}
	
	private void sendMessage(String message) throws Exception {
		
		if (socket == null || keepAlive == null) {
			throw new NetworkException();
		}
		
		socketServerbound.println(message);
		socketServerbound.flush();
		
	}
	
	public void sendPacket(BasicPacket packet) {
		String json = new Gson().toJson(packet);
		
		try {
			if (handshakeCompleted) {
				
				while (true) {
					byte[] secure = Security.encryptAES128(json, aesKey);
					if (secure.length % 16 != 0) {
						packet.aesPadding.add(Byte.MIN_VALUE);
						json = new Gson().toJson(packet);
					}else {
						break;
					}
				}
				sendMessage(new Gson().toJson(new BasicPacket(Security.encryptAES128(json, aesKey))));
			}else {
				sendMessage(json);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void sendPacket(HandshakePacket packet) {
		String json = new Gson().toJson(packet);
		
		try {
			
			sendMessage(json);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void subscribeToInput(NetworkSubscription m) throws Exception {
		
		subs.add(m);
		
	}
	
	public String getAddressAndPort() {
		try {
			return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
		} catch (Exception e) {
			return "";
		}
	}
	
	private Thread createKeepAliveThread() {
		return new Thread("Keep Alive (" + getAddressAndPort() + ")") {
			public void run() {
				while (keepThreadsAlive) {
					
					if ((System.currentTimeMillis()) - lastPing >= 30000) {
						System.out.println("Socket is dead, closing it");
						try {
							closeConnection();
							return;
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					try {
						sendMessage("ping!");
						Thread.sleep(5000);
					} catch (Exception e) {
						System.err.println("Something went wrong with the keep alive thread");
						e.printStackTrace();
					}
				}
			}
		};
	}
	
	private Thread createInputStreamThread() {
		return new Thread("Input Events (" + getAddressAndPort() + ")") {
			@Override
			public void run() {
				while (keepThreadsAlive) {
					while (socketClientBound.hasNext()) {
						
						String message = socketClientBound.nextLine();
						
						if (message.equalsIgnoreCase("ping!")) {
							lastPing = System.currentTimeMillis();
						}else {
							
							if (handshakeCompleted) {
								
								try {
									message = Security.decryptAES128(new Gson().fromJson(message, BasicPacket.class).message, aesKey);
									for (NetworkSubscription sub : subs) {
								    	sub.onEvent(message);
								    }
								} catch (Exception e) {
									e.printStackTrace();
								}
								
							}else {
								HandshakePacket handshakePacket = new Gson().fromJson(message, HandshakePacket.class);
								switch (handshakePacket.stage) {
								case 0:
									try {
										byte publicKeyData[] = handshakePacket.publicKey;
										X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyData);
										KeyFactory kf = KeyFactory.getInstance("RSA");
										PublicKey publicKey = kf.generatePublic(spec);
										sendPacket(new HandshakePacket(Security.encryptRSA(aesKey, publicKey), 1));
										handshakeCompleted = true;
									} catch (Exception e) {
										e.printStackTrace();
									}
									break;
								case 1:
									try {
										aesKey = Security.decryptRSA(handshakePacket.message, Security.rsaKeys.getPrivate());
										handshakeCompleted = true;
									} catch (Exception e) {
										e.printStackTrace();
									}
									break;
								default:
									break;
								}
							}
						}
						
					}
				}
			}
		};
	}
	
	public boolean isConnected() {
		return keepThreadsAlive;
	}
	
}
