package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class AsyncTimeServerHandler implements Runnable{
	//server listen port;
	private int port;
	
	CountDownLatch latch;
	
	AsynchronousServerSocketChannel serverSocketChannel;
	
	public AsyncTimeServerHandler(int port) throws IOException{
		this.port=port;
		
		serverSocketChannel=AsynchronousServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		
		System.out.println("the port "+port+" has been connected");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		latch=new CountDownLatch(1);
		doAccept();
		try{
			latch.await();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	
	public void doAccept(){
		this.serverSocketChannel.accept(this,new AcceptCompletionHandler());
	}
}
