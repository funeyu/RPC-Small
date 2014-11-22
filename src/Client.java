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
	private Connection connection;//���Ǹ��߳��࣬ͬʱ���������й�����send receive�ȹ���
	private static HashMap<Integer, Call> calls = new HashMap<Integer, Call>();
	private static AtomicBoolean running = new AtomicBoolean(true);
	private int callID;           //call�ļ�����ÿ����һ��Զ�̲���callID++
	
	private Client(InetSocketAddress addr) {
		connection=new Connection(addr);
	}

	public static Client build(InetSocketAddress addr) {
		Client client = new Client(addr);
		client.connection.initConnection().start();//initConnection�����������һЩ�������õ�
		return client;
	}
	
	private static class Connection extends Thread {
		private InetSocketAddress addr;
		private SocketChannel channel;
		private Selector selector;

		public Connection(InetSocketAddress addr){
			this.addr=addr;
		}
		
		// ��ʼ��client�����ӣ�����addr ���÷�����
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
				// ɾ����ѡ��key����ֹ�ظ�����
				ite.remove();
				if (key.isConnectable()) {
					channel = (SocketChannel) key.channel();

					// ����������ӣ����������
					if (channel.isConnectionPending()) {
						channel.finishConnect();
					}
					channel.configureBlocking(false);
					// ���ӳɹ���ע����շ�������Ϣ���¼�
					channel.register(selector, SelectionKey.OP_READ);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return this; // Ϊ����ʽд��
		}

		/**
		 * client�˷��͵�����Ϣ��Զ��server��
		 * @param call ���ֽ��������ʽ���� 
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
						// ɾ����ѡ��key����ֹ�ظ�����
						ite.remove();
						if (key.isReadable()) { // �пɶ������¼���
							read(key);
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		
		//������¼��ĺ���
		private void read(SelectionKey key) throws IOException{

			SocketChannel channel = (SocketChannel) key.channel();
			
			ByteBuffer idBuffer=ByteBuffer.allocate(4);
			channel.read(idBuffer);
			idBuffer.flip();
			int callID=idBuffer.getInt();  //��ȡcall��ID��ʾ
			
			ByteBuffer leBuffer=ByteBuffer.allocate(4);
			channel.read(leBuffer);
			leBuffer.flip();
			int bufferLe=leBuffer.getInt();//��ȡ������Ϣ�ĳ��ȣ�bufferLenth-8
			
			ByteBuffer content=ByteBuffer.allocate(bufferLe);
			channel.read(content);		   //����������Ϣ����content��

			System.out.println(callID);
			Call call = calls.get(Integer.valueOf(callID));
			Result result=new Result();
			result.read(content.array());
			
			call.complete(result);
			calls.remove(Integer.valueOf(callID));
		}
	}

	// Client�˵�Call��װ��
	private static class Call {
		private byte[] callInfo;//���͸�server�˵���Ϣ��id,protocol,method,methodParam��
		private int ID;
		public byte[]result;
		
		/**
		 * ��װһ��Call������callInfo
		 * @param param ���õ���Ϣ��"protocol,method,methodParam"
		 * @param id    ���õı�ţ�"id"
		 */
		public Call(byte[] param,int id){
			this.ID=id;
			ByteBuffer buffer=ByteBuffer.allocate(param.length+8);
			buffer.putInt(ID);
			buffer.putInt(param.length);//д��Call���ֽ���
			buffer.put(param);			//��д��call����Ϣ
			buffer.flip();
			this.callInfo=buffer.array();
		}
		//���ý��������ؽ��
		public synchronized void complete(Result value) {
			this.result=value.getResult();
			notify();
		}
		//����Ҫ���͵���Ϣ
		public byte[] getCallInfo(){
			return this.callInfo;
		}
		public byte[] getResult(){
			return this.result;
		}
	}

	//����Զ�̵��õĺ���
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
