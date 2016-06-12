package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO Auto-generated method stub
		cause.printStackTrace();
		if(ctx.channel().isActive()){
			sendError(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private final String url;
	
	public HttpFileServerHandler(String url){
		this.url=url;
	}
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request)
			throws Exception {
		//System.out.println(request.getDecoderResult());
		// TODO Auto-generated method stub
		if(!request.getDecoderResult().isSuccess()){
			System.out.println("bad request");
			sendError(ctx,HttpResponseStatus.BAD_REQUEST);
			return;
		}
		
		if(!request.getMethod().equals(HttpMethod.GET)){
			System.out.println("method not allowed");
			sendError(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}
		
		final String uri=request.getUri();
		System.out.println("uri "+uri);
		String path=sanitizeUri(uri);
		System.out.println("path "+path);
		
		if(path==null){
			System.out.println("forbidden");
			sendError(ctx,HttpResponseStatus.FORBIDDEN);
			return;
		}
		
		File file=new File(path);
		if(file.isHidden()||!file.exists()){
			System.out.println("not found");
			sendError(ctx,HttpResponseStatus.NOT_FOUND);
			return;
		}
		
		if(file.isDirectory()){
			if(uri.endsWith("/")){
				System.out.println("sendlisting");
				sendListing(ctx,file);
			}else{
				sendRedirect(ctx,uri+"/");
			}
			return;
		}
		
		RandomAccessFile randomAccessFile=null;
		try{
			randomAccessFile=new RandomAccessFile(file,"r");
		}catch(FileNotFoundException e){
			sendError(ctx,HttpResponseStatus.NOT_FOUND);
			return;
		}
		
		long fileLength=randomAccessFile.length();
		HttpResponse response=new DefaultHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
		response.headers().setContentLength(response, fileLength);
		setContentTypeHeader(response,file);
		if(request.headers().isKeepAlive(request)){
			response.headers().set(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);
		ChannelFuture future=null;
		future=ctx.write(new ChunkedFile(randomAccessFile,0,fileLength,8192),ctx.newProgressivePromise());
		future.addListener(new ChannelProgressiveFutureListener(){

			@Override
			public void operationProgressed(ChannelProgressiveFuture future,
					long progress, long total) throws Exception {
				// TODO Auto-generated method stub
				if(total<0){
					System.out.println("transfer "+progress);
				}else{
					System.out.println("transfer "+progress+" /. "+total);
				}
			}

			@Override
			public void operationComplete(ChannelProgressiveFuture future)
					throws Exception {
				// TODO Auto-generated method stub
				System.out.println("transfer complete");
			}
			
		});
		
		ChannelFuture lastFuture=ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!request.headers().isKeepAlive(request)){
			lastFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	private static void sendError(ChannelHandlerContext ctx,HttpResponseStatus status){
		FullHttpResponse response=
				new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,status,Unpooled.copiedBuffer("failure"+status.toString()+"\n\r",CharsetUtil.UTF_8));
		response.headers().set("CONTENT_TYPE", "text/plain; charset=utf-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	} 
	
	private static final Pattern INSECURE_URI=Pattern.compile(".*[<>&\"].*");
	
	private String sanitizeUri(String uri){
		try{
			uri=URLDecoder.decode(uri, "utf-8");
			System.out.println("1 : "+uri);
		}catch(UnsupportedEncodingException e){
			try{
				uri=URLDecoder.decode(uri, "ISO-8859-1");
			}catch(UnsupportedEncodingException ee){
				ee.printStackTrace();
			}
		}
		if(!uri.startsWith(url)){
			System.out.println(uri);
			System.out.println("/"+url);
			System.out.println("start here 1");
			return null;
		}
		
		if(!uri.startsWith("/")){
			System.out.println("start here 2");
			return null;
		}
		
		uri=uri.replace('/', File.separatorChar);
		if(uri.contains(File.separator+'.')
				||uri.contains('.'+File.separator)
				||uri.startsWith(".")
				||uri.endsWith(".")
				||INSECURE_URI.matcher(uri).matches()){
			System.out.println("start here 3");
			return null;
		}
		String result=System.getProperty("user.dir")+File.separator+uri;
		System.out.println("result : "+result);
		return result;
	}
	
	public static void sendListing(ChannelHandlerContext ctx,File dir){
		FullHttpResponse response=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE,"text/plain;charset=utf-8");
		StringBuffer sb=new StringBuffer();
		String dirPath=dir.getPath();
		sb.append("<!DOCTYPE HTML>\r\n");
		sb.append("<html>\r\n<head><title>");
		sb.append(dirPath);
		sb.append("Ŀ¼��");
		sb.append("</title></head>\r\n<body>\r\n");
		sb.append("<h3>").append(dirPath).append("Ŀ¼").append("</h3>\r\n");
		sb.append("<ul>\r\n");
		for(File file:dir.listFiles()){
			String name=file.getName();
			sb.append("<li><a href=\"").append(name).append("\">").append(name).append("</a></li>\r\n");
		}
		sb.append("</ul>\r\n</body>\r\n</html>\r\n");
		System.out.println(sb);
		ByteBuf buffer=Unpooled.copiedBuffer(sb, CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		
	}
	
	private static void sendRedirect(ChannelHandlerContext ctx,String newUri){
		FullHttpResponse response=new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.FOUND);
		response.headers().set(HttpHeaders.Names.LOCATION,newUri);
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void setContentTypeHeader(HttpResponse response,File file){
		MimetypesFileTypeMap mimetypeFileTypeMap=new MimetypesFileTypeMap();
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE,mimetypeFileTypeMap.getContentType(file.getPath()));
	}

}
