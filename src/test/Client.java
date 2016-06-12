package test;

import client.AsyncTimeClientHandler;

public class Client {
	public static void main(String args[]){
		new Thread(new AsyncTimeClientHandler("127.0.0.1",5500)).start();
	}
}
