
import java.io.IOException;


public class EchoExecutor implements ExecutorProtocol{

	@Override
	public byte[] onCall(String path) throws IOException {
		
		return path.getBytes();
	}

}
