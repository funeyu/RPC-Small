import java.io.IOException;

/**
 * ���Ǹ�Զ�̵��õĽӿ�
 * @author Administrator
 *
 */
public interface ExecutorProtocol {
	
	//Զ�̵��õ�һ��ִ�к���
	public byte[] onCall(String path) throws IOException;
}
