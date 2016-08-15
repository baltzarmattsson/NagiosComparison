
public class EraserThread implements Runnable {
	private boolean stop;

	/**
	 *  The
	 *            prompt displayed to the user
	 */
	public EraserThread(String prompt) {
		System.out.print(prompt);
	}

	/**
	 * Begin masking...display asterisks (*)
	 */
	public void run() {
		System.out.println();
		stop = false;
		while (!stop) {
			System.out.print("\010*");
			try {
				Thread.currentThread().sleep(1);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	/**
	 * Instruct the thread to stop masking
	 */
	public void stopMasking() {
		this.stop = true;
	}
}