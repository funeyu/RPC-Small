import java.io.IOException;

/**
 * 这是个远程调用的接口
 * @author Administrator
 *
 */
public interface ExecutorProtocol {
	
	//远程调用的一个执行函数
	public byte[] onCall(String path) throws IOException;
}
