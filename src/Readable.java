import java.io.Serializable;


public interface Readable extends Serializable {

	public void read(byte[] input);
	public void write(byte[] output);
	public byte[] getBytes();
}
