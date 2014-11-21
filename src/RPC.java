import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;


public class RPC {
	
	private static Server server;
	private RPC(){}

	private static class Invocation implements Callable{
		private String methodName;    
		private String methodParam;    //�򻯴����ˣ�methodParam����ȫ��ΪString���͵ģ���ֻ��һ������
		private String protocol;       //��Ǹõ��õĽӿ�����
		
		public Invocation(String method,String param,String protocol){
			this.methodName=method;
			this.methodParam=param;
			this.protocol=protocol;
		}

		public byte[] bytes(){
			return (methodName+","+methodParam+","+protocol).getBytes();
		}
		@Override
		public String getMethodName() {
			
			return null;
		}

		@Override
		public String getProtocolName() {
			
			return null;
		}

		@Override
		public String getMethodParam() {
			
			return null;
		}
		
		
	}
	
	public static class invoker implements InvocationHandler{

		private  static Client client;
		private String protocol;     //Ҫ�����������
		
		public invoker(InetSocketAddress addr,String protocol){
			
			if(client==null){
				this.client=Client.build(addr);
			}
			this.protocol=protocol;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			//����method���������һ�����ã��򻯴���ֻ��һ��String���Ͳ���
			Invocation invocation=new Invocation(method.getName(), (String)args[0],protocol);
			return client.call(invocation.bytes());
		} 
	}
	
	public static ExecutorProtocol getProxy(
			Class<?> protocol,InetSocketAddress addr ){
		
		return (ExecutorProtocol) Proxy.newProxyInstance(
				protocol.getClassLoader(), protocol.getInterfaces(), 
				new invoker(addr,protocol.getName()));
	}
	
	
	public static void setProxy(Class<?extends ExecutorProtocol>protocol){
		server.setProxy(protocol);
	}
	
	public static void setProxyForServer(Class<?extends ExecutorProtocol>protocol,int port){
		(server=Server.initServer(port, protocol))
		.listen();
	}
}
