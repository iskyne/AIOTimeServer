package test;

import java.io.IOException;

import server.AsyncTimeServerHandler;

public class Server {
	public static void main(String args[]){
		try {
			new Thread(new AsyncTimeServerHandler(5500)).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
