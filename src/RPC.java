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
		private String methodParam;    //简化处理了，methodParam这里全部为String类型的，且只有一个参数
		private String protocol;       //标记该调用的接口名称
		
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
		private String protocol;     //要代理的类名称
		
		public invoker(InetSocketAddress addr,String protocol){
			
			if(client==null){
				this.client=Client.build(addr);
			}
			this.protocol=protocol;
		}
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			//根据method与参数创建一个调用，简化处理：只有一个String类型参数
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
