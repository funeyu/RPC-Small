
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
			=new ConcurrentHashMap<String,Class<?>>();//�������������

	private static ConcurrentHashMap<Class<?>,Object>objectCache
			=new ConcurrentHashMap<Class<?>, Object>();//�������ʵ�����Ķ���
	
	private  int port;				   //�����Ķ˿ں�
	private AtomicBoolean listenning = new AtomicBoolean(true); // ����listenner���Ƿ���������ı�־λ
	private static Server singleServer;                         // ���ɵ���ģʽ��ֻ��init�����´�������ֵ��

	/**
	 * ��ʼ��Serverֵ�����󶨶˿ں�port
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
	 * Server�˸���protocol���趨������
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
		// ��һ�Ĵ��������̣߳����������߳����ó�һ���߳�������
		new Listenner().start();
	}

	/**
	 * �������ӵ��߳�
	 * 
	 * @author Administrator
	 *
	 */
	private class Listenner extends Thread {

		private ServerSocketChannel acceptChannel = null;
		private Selector selector = null;

		/**
		 * �����߳���Ҫ������
		 */
		public void run() {
			try {
				acceptChannel = ServerSocketChannel.open();// ���ɼ�����
				acceptChannel.configureBlocking(false); // ���ó�һ��ģʽ
				selector = Selector.open(); // �����źż�����
				acceptChannel.socket().bind(new InetSocketAddress(port));
				acceptChannel.register(selector, SelectionKey.OP_ACCEPT);

				while (listenning.get()) { // һ��while true��ѭ����

					int n = selector.select();
					if (n == 0) { // û��ָ����I/O�¼�����
						continue;
					}
					Iterator<SelectionKey> it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey) it.next();
						if (key.isAcceptable()) {// ĳsocket�����ź�
							ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
									.channel();
							SocketChannel socketChannel = serverSocketChannel
									.accept();
							socketChannel.configureBlocking(false);
							socketChannel.register(selector,
									SelectionKey.OP_READ);
						}
						if (key.isReadable() && key.isValid()) {// ĳsocket�ɶ��ź�
							doRead(key); // ����read
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
	 * ��ȡ�ͻ��˷�������Ϣ
	 * 
	 * @param key
	 * @throws IOException
	 */
	public void doRead(SelectionKey key) throws IOException {
		// �������ɶ���Ϣ���õ��¼�������Socketͨ��
		SocketChannel channel = (SocketChannel) key.channel();

		try {
			ByteBuffer idBuffer = ByteBuffer.allocate(4);
			channel.read(idBuffer);
			idBuffer.flip();
			int callID = idBuffer.getInt(); // ��ȡcall��ID��ʾ

			ByteBuffer leBuffer = ByteBuffer.allocate(4);
			channel.read(leBuffer);
			leBuffer.flip();
			int bufferLe = leBuffer.getInt();// ��ȡ������Ϣ�ĳ��ȣ�bufferLenth-8

			ByteBuffer content = ByteBuffer.allocate(bufferLe);
			channel.read(content);           // ����������Ϣ����content��
			
			handleCall(channel,content.array(),callID);
		} catch (IOException e) {
			key.cancel();
			channel.socket().close();
			channel.close();
			return;
		}
	}
	
	/**
	 * ����client�˵�Զ�̵���
	 * @param channel   
	 * @param callInfo client�˵���call��������Ϣ
	 * @param callID   call�ı�ʾ
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