package server;

import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class TimeServerHandler extends SimpleChannelInboundHandler{
	private int counter=0;
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		ctx.flush();
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
		String body=(String) arg1;
		System.out.println("the time server receive the order : "+body+" "+(++counter));
		String currentTime=body.equalsIgnoreCase("time required")?new Date(System.currentTimeMillis()).toString():"bad order";
		//System.out.println(currentTime);
		ByteBuf response=Unpooled.copiedBuffer(currentTime.getBytes());
		arg0.write(response);
	}
}
