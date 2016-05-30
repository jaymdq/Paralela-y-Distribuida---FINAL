package montecarlo.Better;

import jgfutil.JGFInstrumentor;


public class Main {

	private static int cantidadRepeticiones = 5;
	private static int limiteSuperiorDeThreads = 30;
	
	public static void main(String[] args) {
				
		for (int i = 0; i < 5; i++){
			System.out.println("No considerar");
			String[] params = {"1"};
			JGFMonteCarloBenchSizeB3_Better.main(params);
			// Se borran los timers
			JGFInstrumentor.clearTimers();
		}
		
		System.out.println("Si considerar");
		
		// Variación de cantidad de threads
		for (int nthreads = 28; nthreads <= limiteSuperiorDeThreads; nthreads++){
			System.out.println("\nCantidad de Threads: " + nthreads);
			
			// Variación de cantidad de repeticiones
			for (int rep = 1; rep <= cantidadRepeticiones; rep++){
				System.out.println("Repetición: " + rep);
				
				String[] params = {""+nthreads};
				JGFMonteCarloBenchSizeB3_Better.main(params);				
				
				// Se borran los timers
				JGFInstrumentor.clearTimers();
			}
		}
		
	}
}
