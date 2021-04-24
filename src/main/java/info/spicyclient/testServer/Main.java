package info.spicyclient.testServer;

import info.spicyclient.testServer.networking.NetworkException;
import info.spicyclient.testServer.networking.NetworkConnection;
import info.spicyclient.testServer.networking.NetworkReceiver;
import info.spicyclient.testServer.networking.NetworkSubscription;

public class Main {

	public static void main(String[] args) {
		try {
			new NetworkReceiver().openConnection(3000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
