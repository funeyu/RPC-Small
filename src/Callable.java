
public interface Callable {
	/**
	 * 获取调用的方法名的函数
	 * @return
	 */
	public String getMethodName();
	
	/**
	 * 获取调用的接口实现名称
	 * @return
	 */
	public String getProtocolName();
	
	/**
	 * 获取方法的执行参数，这里简化只有一个参数的情况
	 * @return
	 */
	public String getMethodParam();

}