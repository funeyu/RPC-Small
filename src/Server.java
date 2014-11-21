
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

	private static ConcurrentHashMap<String,Class<?>>classCache
			=new ConcurrentHashMap<String,Class<?>>();//缓存代理对象的类

	private static ConcurrentHashMap<Class<?>,Object>objectCache
			=new ConcurrentHashMap<Class<?>, Object>();//缓存代理实例化的对象
	
	private  int port;				   //监听的端口号
	private AtomicBoolean listenning = new AtomicBoolean(true); // 设置listenner的是否继续监听的标志位
	private static Server singleServer;                         // 做成单列模式，只在init函数下创建返回值；

	/**
	 * 初始化Server值，并绑定端口号port
	 */
	public static Server initServer(int ports,Class<?extends ExecutorProtocol>protocol) {
		if (singleServer == null) {
			singleServer = new Server();
		}
		try {
			if(objectCache.get(protocol)==null){
				objectCache.put(protocol, protocol.newInstance());
				classCache.put(protocol.getName(), protocol);
			}
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		singleServer.port = ports;
		return singleServer;
	}
	
	/**
	 * Server端根据protocol来设定代理类
	 * @param protocol
	 */
	public void setProxy(Class<?extends ExecutorProtocol>protocol){
		
		if(objectCache.get(protocol)==null){
			try {
				objectCache.put(protocol, protocol.newInstance());
				classCache.put(protocol.getName(), protocol);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} 
	}

	public void listen() {
		// 单一的处理事务线程，这里事务线程设置成一个线程来监听
		new Listenner().start();
	}

	/**
	 * 监听连接的线程
	 * 
	 * @author Administrator
	 *
	 */
	private class Listenner extends Thread {

		private ServerSocketChannel acceptChannel = null;
		private Selector selector = null;

		/**
		 * 监听线程主要的任务
		 */
		public void run() {
			try {
				acceptChannel = ServerSocketChannel.open();// 生成监听端
				acceptChannel.configureBlocking(false); // 设置成一部模式
				selector = Selector.open(); // 生成信号监听器
				acceptChannel.socket().bind(new InetSocketAddress(port));
				acceptChannel.register(selector, SelectionKey.OP_ACCEPT);

				while (listenning.get()) { // 一个while true的循环体

					int n = selector.select();
					if (n == 0) { // 没有指定的I/O事件发生
						continue;
					}
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey) it.next();
						if (key.isAcceptable()) {// 某socket连接信号
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
									.channel();
							SocketChannel socketChannel = serverSocketChannel
									.accept();
							socketChannel.configureBlocking(false);
							socketChannel.register(selector,
									SelectionKey.OP_READ);
						}
						if (key.isReadable() && key.isValid()) {// 某socket可读信号
							doRead(key); // 处理read
						}
						it.remove();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * 读取客户端发来的信息
	 * 
	 * @param key
	 * @throws IOException
	 */
	public void doRead(SelectionKey key) throws IOException {
		// 服务器可读消息：得到事件发生的Socket通道
		SocketChannel channel = (SocketChannel) key.channel();

		try {
			ByteBuffer idBuffer = ByteBuffer.allocate(4);
			channel.read(idBuffer);
			idBuffer.flip();
			int callID = idBuffer.getInt(); // 获取call的ID标示

			ByteBuffer leBuffer = ByteBuffer.allocate(4);
			channel.read(leBuffer);
			leBuffer.flip();
			int bufferLe = leBuffer.getInt();// 获取主体信息的长度：bufferLenth-8

			ByteBuffer content = ByteBuffer.allocate(bufferLe);
			channel.read(content);           // 读出主体信息存于content中
			
			handleCall(channel,content.array(),callID);
		} catch (IOException e) {
			key.cancel();
			channel.socket().close();
			channel.close();
			return;
		}
	}
	
	/**
	 * 处理client端的远程调用
	 * @param channel   
	 * @param callInfo client端调用call的主体信息
	 * @param callID   call的标示
	 */
	public void handleCall(SocketChannel channel,byte[] callInfo,int callID){
		
		String call=new String(callInfo);
		String name=call.split(",")[0];
		String paramete=call.split(",")[1];
		String className=call.split(",")[2];
		try {
			Method method=classCache.get(className)
					      .getMethod(name, String.class);
			Object protocol=objectCache.get(classCache.get(className));
			byte[] result=(byte[])method.invoke(protocol, paramete);
			ByteBuffer buffer=ByteBuffer.allocate(result.length+8);
			
			buffer.putInt(callID);
			buffer.putInt(result.length);
			buffer.put(result);
			buffer.flip();
			
			channel.write(buffer);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}