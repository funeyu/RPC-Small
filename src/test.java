
import java.io.IOException;
import java.net.InetSocketAddress;


public class test {

	public  static void main(String[]args) throws IOException{
		//��ʼ��һ��������server������ʼEchoExecutor����
		RPC.setProxyForServer(EchoExecutor.class, 18000);
		//server������һ��ReadFile����
		RPC.setProxy(ReadFileExecutor.class);
		
		//.....
		
		ExecutorProtocol echo=RPC.getProxy(EchoExecutor.class, new InetSocketAddress("127.0.0.1", 18000));
		ExecutorProtocol reader=RPC.getProxy(ReadFileExecutor.class, new InetSocketAddress("127.0.0.1", 18000));
		
		
		byte[]result=echo.onCall("hello !!!!!");
		System.out.println("result:"+new String(result));
		
		System.out.println(new String(reader.onCall("f://objectdb.conf")));
		
	}
}
