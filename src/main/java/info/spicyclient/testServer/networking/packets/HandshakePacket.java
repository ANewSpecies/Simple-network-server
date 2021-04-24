package info.spicyclient.testServer.networking.packets;

public class HandshakePacket {
	
	public HandshakePacket(String message, int stage) {
		this.message = message;
		this.stage = stage;
	}
	
	public HandshakePacket(byte[] publicKey, int stage) {
		this.publicKey = publicKey;
		this.stage = stage;
	}
	
	public HandshakePacket(String message, byte[] publicKey, int stage) {
		this.message = message;
		this.publicKey = publicKey;
		this.stage = stage;
	}
	
	public int stage;
	public String message;
	public byte[] publicKey;
	
}
