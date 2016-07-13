package montecarlo;

import jgfutil.JGFInstrumentor;


public class Main {

	private static int cantidadRepeticiones = 5;
	private static int limiteSuperiorDeThreads = 16;
	
	public static void main(String[] args) {
				
		for (int i = 0; i < 5; i++){
			System.out.println("No considerar");
			String[] params = {"1"};
			JGFMonteCarloBenchSizeB3_Better.main(params);
			// Se borran los timers
			JGFInstrumentor.clearTimers();
		}
		
		System.out.println("Si considerar");
		
		// Variaci�n de cantidad de threads
		for (int nthreads = 1; nthreads <= limiteSuperiorDeThreads; nthreads++){
			System.out.println("\nCantidad de Threads: " + nthreads);
			
			// Variaci�n de cantidad de repeticiones
			for (int rep = 1; rep <= cantidadRepeticiones; rep++){
				System.out.println("Repetici�n: " + rep);
				
				String[] params = {""+nthreads};
				JGFMonteCarloBenchSizeB3_Better.main(params);				
				
				// Se borran los timers
				JGFInstrumentor.clearTimers();
			}
		}
		
	}
}
