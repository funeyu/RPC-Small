import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;


public class RPC {
	
	private static Server server;
	private static Client client;
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
		private String protocol;     //Ҫ�����������
		
		public invoker(String protocol){
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
		if(client==null){
			client=Client.build(addr);
		}
		return (ExecutorProtocol) Proxy.newProxyInstance(
				protocol.getClassLoader(), protocol.getInterfaces(), 
				new invoker(protocol.getName()));
	}
	
	/**
	 * 
	 * @param protocol
	 */
	public static void setProxy(Class<?extends ExecutorProtocol>protocol){
		server.setProxy(protocol);
	}
	
	public static void setProxyForServer(Class<?extends ExecutorProtocol>protocol,int port){
		(server=Server.initServer(port, protocol))
		.listen();
	}
}
