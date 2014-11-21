import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public class ReadFileExecutor implements ExecutorProtocol{

	private String path;
	private int capacity=1000;
	private ByteBuffer buffer;
	
	@Override
	public byte[] onCall(String path) throws IOException {
		
		RandomAccessFile file = new RandomAccessFile(path, "rw");
		FileChannel readin=file.getChannel();
		if(capacity<file.length())
			buffer=ByteBuffer.allocate(capacity);
		else
			buffer=ByteBuffer.allocate((int)file.length());
		readin.read(buffer);
		return buffer.array();
	}
	
	
}
