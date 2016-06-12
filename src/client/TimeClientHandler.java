package client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TimeClientHandler extends SimpleChannelInboundHandler{
	private PrintWriter pw=null;
	private byte[] buffer="time required".getBytes();
	private int counter=0;
	public TimeClientHandler(){
		try {
			pw=new PrintWriter(new FileWriter(
					new File("E:\\IskyneWorkSpace\\MyEclipse\\AIOTimeServer\\src\\client\\log"),true));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0;i<100;i++){
			ByteBuf request=Unpooled.copiedBuffer(buffer);
			ctx.writeAndFlush(request);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Auto-generated method stub
		ctx.close();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO Auto-generated method stub
		String currentTime=(String) arg1;
		System.out.println("the current time :"+currentTime+ ++counter);
		pw.println("the current time :"+currentTime+" "+counter);
		pw.flush();
		//pw.close();
	}
}
