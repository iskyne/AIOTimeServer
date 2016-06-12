package client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class AsyncTimeClientHandler implements CompletionHandler<Void,AsyncTimeClientHandler>,Runnable{
	
	private int port;
	private AsynchronousSocketChannel socketChannel;
	private String host;
	private CountDownLatch latch;
	
	public AsyncTimeClientHandler(String host,int port){
		this.port=port;
		this.host=host;
		try {
			socketChannel=AsynchronousSocketChannel.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		latch=new CountDownLatch(1);
		socketChannel.connect(new InetSocketAddress(host,port), this, this);
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void completed(Void result, AsyncTimeClientHandler attachment) {
		// TODO Auto-generated method stub
		byte[] bytes="Query time".getBytes();
		ByteBuffer buffer=ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		attachment.socketChannel.write(buffer, buffer, new CompletionHandler<Integer,ByteBuffer>(){

			@Override
			public void completed(Integer result, ByteBuffer attachment) {
				// TODO Auto-generated method stub
				if(attachment.hasRemaining()){
					socketChannel.write(attachment, attachment, this);
				}else{
					ByteBuffer readBuffer=ByteBuffer.allocate(1024);
					socketChannel.read(readBuffer, readBuffer, new CompletionHandler<Integer,ByteBuffer>(){

						@Override
						public void completed(Integer result,
								ByteBuffer attachment) {
							// TODO Auto-generated method stub
							attachment.flip();
							byte[] buffer=new byte[attachment.remaining()];
							attachment.get(buffer);
							
							try {
								System.out.println(new String(buffer,"utf-8"));
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							latch.countDown();
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							// TODO Auto-generated method stub
							try {
								socketChannel.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							latch.countDown();
						}
						
					});
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				// TODO Auto-generated method stub
				try {
					socketChannel.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				latch.countDown();
			}
			
		});
	}

	@Override
	public void failed(Throwable exc, AsyncTimeClientHandler attachment) {
		// TODO Auto-generated method stub
		exc.printStackTrace();
		try {
			latch.countDown();
			socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
