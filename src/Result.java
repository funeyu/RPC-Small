

public class Result implements Readable{

	private byte[]data;
	
	public byte[] getResult(){
		return data;
	}

	@Override
	public byte[] getBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * ����Զ�̵��õĽ���ֽ�
	 */
	@Override
	public void read(byte[] input) {
		this.data=input;
		
	}

	@Override
	public void write(byte[] output) {
		
		
	}
}
