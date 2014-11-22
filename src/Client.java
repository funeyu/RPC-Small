import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * 
 * @author Administrator
 *
 */
public class Client {
	private Connection connection;//这是个线程类，同时处理所有有关网络send receive等过程
	private static HashMap<Integer, Call> calls = new HashMap<Integer, Call>();
	private static AtomicBoolean running = new AtomicBoolean(true);
	private int callID;           //call的计数，每调用一个远程操作callID++
	
	private Client(InetSocketAddress addr) {
		connection=new Connection(addr);
	}

	public static Client build(InetSocketAddress addr) {
		Client client = new Client(addr);
		client.connection.initConnection().start();//initConnection这里可以配置一些参数设置的
		return client;
	}
	
	private static class Connection extends Thread {
		private InetSocketAddress addr;
		private SocketChannel channel;
		private Selector selector;

		public Connection(InetSocketAddress addr){
			this.addr=addr;
		}
		
		// 初始化client端连接，设置addr 设置非阻塞
		private Connection initConnection() {
			try {
				channel = SocketChannel.open();
				channel.configureBlocking(false);
				selector = Selector.open();
				channel.connect(addr);
				channel.register(selector, SelectionKey.OP_CONNECT);

				selector.select();
				Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
				SelectionKey key = ite.next();
				// 删除已选的key，防止重复处理
				ite.remove();
				if (key.isConnectable()) {
					channel = (SocketChannel) key.channel();

					// 如果正在连接，则完成连接
					if (channel.isConnectionPending()) {
						channel.finishConnect();
					}
					channel.configureBlocking(false);
					// 连接成功后，注册接收服务器消息的事件
					channel.register(selector, SelectionKey.OP_READ);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return this; // 为了链式写法
		}

		/**
		 * client端发送调用信息到远程server端
		 * @param call 以字节数组的形式发送 
		 * {callID bufferLenth-8  protocol,methodName,methodParam}.getBytes()
		 * @throws IOException
		 */
		public void sendCall(byte[] call) throws IOException{
			channel.write(ByteBuffer.wrap(call));
		}
		@Override
		public void run() {
			for (; running.get();) {
				try {
					selector.select();
					Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
					while (ite.hasNext()) {
						SelectionKey key = ite.next();
						// 删除已选的key，防止重复处理
						ite.remove();
						if (key.isReadable()) { // 有可读数据事件。
							read(key);
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		
		//处理读事件的函数
		private void read(SelectionKey key) throws IOException{

			SocketChannel channel = (SocketChannel) key.channel();
			
			ByteBuffer idBuffer=ByteBuffer.allocate(4);
			channel.read(idBuffer);
			idBuffer.flip();
			int callID=idBuffer.getInt();  //获取call的ID标示
			
			ByteBuffer leBuffer=ByteBuffer.allocate(4);
			channel.read(leBuffer);
			leBuffer.flip();
			int bufferLe=leBuffer.getInt();//获取主体信息的长度：bufferLenth-8
			
			ByteBuffer content=ByteBuffer.allocate(bufferLe);
			channel.read(content);		   //读出主体信息存于content中

			System.out.println(callID);
			Call call = calls.get(Integer.valueOf(callID));
			Result result=new Result();
			result.read(content.array());
			
			call.complete(result);
			calls.remove(Integer.valueOf(callID));
		}
	}

	// Client端的Call封装，
	private static class Call {
		private byte[] callInfo;//发送给server端的信息“id,protocol,method,methodParam”
		private int ID;
		public byte[]result;
		
		/**
		 * 封装一个Call，填充好callInfo
		 * @param param 调用的信息："protocol,method,methodParam"
		 * @param id    调用的编号："id"
		 */
		public Call(byte[] param,int id){
			this.ID=id;
			ByteBuffer buffer=ByteBuffer.allocate(param.length+8);
			buffer.putInt(ID);
			buffer.putInt(param.length);//写入Call的字节数
			buffer.put(param);			//再写入call的信息
			buffer.flip();
			this.callInfo=buffer.array();
		}
		//调用结束并返回结果
		public synchronized void complete(Result value) {
			this.result=value.getResult();
			notify();
		}
		//返回要发送的信息
		public byte[] getCallInfo(){
			return this.callInfo;
		}
		public byte[] getResult(){
			return this.result;
		}
	}

	//发送远程调用的函数
	public byte[] call(byte[] call){
		callID++;
		Call acall=new Call(call,callID);
		calls.put(Integer.valueOf(callID),acall);
		synchronized (acall) {
			try {
				this.connection.sendCall(acall.getCallInfo());
				acall.wait();
				return acall.getResult();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}

}
