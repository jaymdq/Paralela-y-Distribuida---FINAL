package brian;

public class Main {

	public static void main(String[] args) {

		int n_Task = 1;
		
		try {
			Work worker = new Work(n_Task);
			worker.work();
			worker.validateResults();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
