package brian;

import jgfutil.JGFInstrumentor;
import moldyn.Better.JGFMolDynBenchSizeB_Better;

public class Main {

	private static int cantidadRepeticiones = 5;
	private static int limiteSuperiorDeThreads = 16;
	
	public static void main(String[] args) {

		// Warming Up
		/*for (int i = 0; i < 5; i++){
			System.out.println("No considerar " + i);
			
			try{
			Work worker = new Work(2);
			worker.work();
			worker.validateResults();
			} catch (Exception e){
				e.printStackTrace();
			}
			
			// Se borran los timers
			JGFInstrumentor.clearTimers();
		}*/

		// Variación de cantidad de threads
		for (int nTasks = 2; nTasks <= limiteSuperiorDeThreads; nTasks++){
			System.out.println("\nCantidad de Tasks: " + nTasks);

			// Variación de cantidad de repeticiones
			for (int rep = 1; rep <= cantidadRepeticiones; rep++){
				System.out.println("Repetición: " + rep);
				
				try {
					JGFInstrumentor.addTimer("Section3:MolDyn:Total");
					JGFInstrumentor.startTimer("Section3:MolDyn:Total");
					
					Work worker = new Work(nTasks);
					worker.work();
					worker.validateResults();
					
					JGFInstrumentor.stopTimer("Section3:MolDyn:Total");
					
				    JGFInstrumentor.printTimer("Section3:MolDyn:Run"); 
				    JGFInstrumentor.printTimer("Section3:MolDyn:Total"); 
				    
				    //GC
				    System.gc();
					
				} catch (Exception e) {
					e.printStackTrace();
				}			

				// Se borran los timers
				JGFInstrumentor.clearTimers();
				return;
			}
		}


		
	}

}
