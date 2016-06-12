package server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ReadCompletionHandler implements CompletionHandler<Integer,ByteBuffer>{

	private AsynchronousSocketChannel socketChannel;
	
	ReadCompletionHandler(AsynchronousSocketChannel socketChannel){
		this.socketChannel=socketChannel;
	}
	@Override
	public void completed(Integer result, ByteBuffer attachment) {
		// TODO Auto-generated method stub
		attachment.flip();
		byte[] body=new byte[attachment.remaining()];
		attachment.get(body);
		
		try{
			String req=new String(body,"UTF-8");
			System.out.println("The time server receive order :"+req);
			String currentTime="Query time".equalsIgnoreCase(req)?(new java.util.Date(System.currentTimeMillis()).toString()):"Bad req";
			doWrite(currentTime);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		// TODO Auto-generated method stub
		exc.printStackTrace();
		try {
			this.socketChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void doWrite(String response){
		if(response!=null&&response.trim().length()>0){
			byte[] bytes=response.getBytes();
			ByteBuffer writeBuffer=ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			writeBuffer.flip();
			this.socketChannel.write(writeBuffer, writeBuffer, new CompletionHandler<Integer,ByteBuffer>(){

				@Override
				public void completed(Integer result, ByteBuffer attachment) {
					// TODO Auto-generated method stub
					if(attachment.hasRemaining()){
						socketChannel.write(attachment, attachment, this);
					}
				}

				@Override
				public void failed(Throwable exc, ByteBuffer attachment) {
					// TODO Auto-generated method stub
					exc.printStackTrace();
					try {
						socketChannel.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		}
	}
}
