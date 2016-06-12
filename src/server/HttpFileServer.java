package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServer {
	public void bind(int port ,final String url) throws Exception{
		EventLoopGroup boss=new NioEventLoopGroup();
		EventLoopGroup works=new NioEventLoopGroup();
		
		try{
			ServerBootstrap b=new ServerBootstrap();
			b.group(boss,works).channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>(){

				@Override
				protected void initChannel(SocketChannel arg0) throws Exception {
					// TODO Auto-generated method stub
					arg0.pipeline().addLast(new HttpRequestDecoder());
					arg0.pipeline().addLast(new HttpObjectAggregator(65536));
					arg0.pipeline().addLast(new HttpResponseDecoder());
					arg0.pipeline().addLast(new ChunkedWriteHandler());
					arg0.pipeline().addLast(new HttpFileServerHandler(url));
				}
			});
			
			ChannelFuture f=b.bind("127.0.0.1",port).sync();
			f.channel().closeFuture().sync();
		}finally{
			boss.shutdownGracefully();
			works.shutdownGracefully();
		}
	}
	
	public static void main(String args[]){
		try {
			new HttpFileServer().bind(8080, "/src");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
