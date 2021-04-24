package info.spicyclient.testServer.networking.packets;

import java.util.ArrayList;

public class BasicPacket {
	
	public BasicPacket(byte[] message) {
		this.message = message;
	}
	
	public BasicPacket(String payload1, String payload2) {
		this.payload1 = payload1;
		this.payload2 = payload2;
	}
	
	public byte[] message;
	
	public String payload1, payload2;
	public ArrayList<Byte> aesPadding = new ArrayList<Byte>();
	
}
