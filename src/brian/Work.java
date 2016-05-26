package brian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.client.Operator;
import org.jppf.client.event.JobEvent;
import org.jppf.client.event.JobListenerAdapter;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

import jgfutil.JGFInstrumentor;

public class Work {

	// Variables de la clase Work
	private ArrayList<SuperTask> tasks = new ArrayList<SuperTask>();

	// Variables relacionadas al problema

	private static final int ITERS = 100;
	private static final double LENGTH = 50e-10;
	private static final double m = 4.0026;
	private static final double mu = 1.66056e-27;
	private static final double kb = 1.38066e-23;
	private static final double TSIM = 50;
	private static final double deltat = 5e-16;
	private static double den = 0.83134;
	private static double tref = 0.722;
	private static double h = 0.064;
	private static int interactions = 0;
	private static double count = 0.0;

	private static double [] epot;
	private static double [] vir;
	private static double [] ek;
	private static int [] interacts;

	private static int mm = 13;
	private static int PARTSIZE = mm*mm*mm*4;

	// Variables del Runnable
	// Del Runnable
	private int i,j,k,lg,mdsize,move;

	private double l,rcoff,rcoffs,side,sideh,hsq,hsq2,vel,velt;
	private double a,r,sum,tscale,sc,ekin,ts,sp;
	private double vaver,vaverh,rand;
	private double etot,temp,pres,rp;
	private double u1,u2,v1,v2,s, xx, yy, zz;
	private double xvelocity, yvelocity, zvelocity;

	private double [][] sh_force;
	private double [][][] sh_force2;

	private int ijk,npartm,iseed,tint;
	private static int irep = 10;
	private static int istop = 19;
	private static int iprint = 10;
	private static int movemx = 50;

	private Random randnum;

	private Particle one [] = null;

	private int n_Task;

	private MemoryMapDataProvider dataProvider;

	private JPPFJob job;

	// Constructors

	public Work(int n_Task) {

		this.n_Task = n_Task;
		//TODO Meter la inicialización aca

	}

	// Getters and Setters

	// Methods

	public void work() throws Exception{

		// Accedo al cliente
		try (final JPPFClient jppfClient = new JPPFClient()) {

			// Me aseguro tener suficientes conexiones
			ensureSufficientConnections(jppfClient, 1);

			// Seteo de datos
			epot = new double [n_Task];
			vir = new double [n_Task];
			ek = new double [n_Task];
			interacts = new int [n_Task];
			sh_force = new double[3][PARTSIZE];
			sh_force2 = new double[3][n_Task][PARTSIZE];

			// Difusión de los datos 
			// Aclaración: el dataProvider tiene que ser creado y rellenado antes de crear el Job.
			dataProvider = new MemoryMapDataProvider();
			populateDataProvider();

			// Se crea el trabajo 1
			JPPFJob job = createJob_1(n_Task);

			// Determinamos que el Job sea bloqueante (Puede omitirse)
			job.setBlocking(true);

			job.addJobListener(new JobListenerAdapter() {
				@Override
				public synchronized void jobEnded(final JobEvent event) {
					processResults2(event.getJob());
				}
			});
						
			// Parte 1 -> Distribuido
			jppfClient.submitJob(job);

			// Trabajo del thread principal
			jgfutil.JGFInstrumentor.addTimer("Section3:MolDyn:Run");
			jgfutil.JGFInstrumentor.startTimer("Section3:MolDyn:Run");

			// For - Delegado al thread principal

			move = 0;
			for (move = 0; move < movemx; move++) {

				// Parte 2 -> Distribuido
				updateTaskStates(SuperTask.STATE.PART_2);
				jppfClient.submitJob(job);

				// Trabajo del thread principal
				for( j = 0; j < 3; j++) {
					for (i = 0; i < mdsize; i++) {
						sh_force[j][i] = 0.0;
					}
				}

				// Parte 3 -> Distribuido
				updateTaskStates(SuperTask.STATE.PART_3);
				jppfClient.submitJob(job);

				// Trabajo del thread principal
				for(int k = 0; k < 3; k++) {
					for(i = 0 ; i < mdsize; i++) {
						for( j = 0; j < /*JGFMolDynBench_Better.nthreads*/ n_Task; j++) {
							sh_force[k][i] += sh_force2[k][j][i];
						}
					}
				}

				for(int k = 0; k < 3; k++) {
					for(i = 0; i < mdsize; i++) {
						for( j = 0; j < /*JGFMolDynBench_Better.nthreads*/ n_Task; j++) {
							sh_force2[k][j][i] = 0.0;
						}
					}
				}

				for( j = 1; j < /*JGFMolDynBench_Better.nthreads*/ n_Task; j++) {
					//md_Better.epot[0] += md_Better.epot[j];
					//md_Better.vir[0] += md_Better.vir[j];
					epot[0] += epot[j];
					vir[0] += vir[j];
				}
				for( j = 1; j < /*JGFMolDynBench_Better.nthreads*/ n_Task; j++) {       
					//md_Better.epot[j] = md_Better.epot[0];
					//md_Better.vir[j] = md_Better.vir[0];
					epot[j] = epot[0];
					vir[j] = vir[0];
				}
				for( j = 0; j < /*JGFMolDynBench_Better.nthreads*/ n_Task; j++) {
					//md_Better.interactions += md_Better.interacts[j];
					interactions += interacts[j];
				}


				// Trabajo del thread principal
				for ( j = 0 ; j < 3; j++) {
					for (i = 0; i < mdsize; i++) {
						sh_force[j][i] = sh_force[j][i] * hsq2;
					}
				}

				// Parte 4 -> Distribuido
				updateTaskStates(SuperTask.STATE.PART_4);
				jppfClient.submitJob(job);

			}

			// Trabajo del thread principal
			JGFInstrumentor.stopTimer("Section3:MolDyn:Run");
		}

		System.out.println("Todos los Trabajos finalizaron");

	}

	private void populateDataProvider() {

		dataProvider.setParameter("epot", epot);
		dataProvider.setParameter("vir", vir);
		dataProvider.setParameter("ek", ek);
		dataProvider.setParameter("interacts", interacts);
		dataProvider.setParameter("sh_force", sh_force);
		dataProvider.setParameter("sh_force2", sh_force2);
		dataProvider.setParameter("PARTSIZE",PARTSIZE);
		dataProvider.setParameter("den",den);
		dataProvider.setParameter("mm",mm);
		dataProvider.setParameter("side",side);
		dataProvider.setParameter("hsq",hsq);
		dataProvider.setParameter("mdsize",mdsize);
		if (one != null)
			dataProvider.setParameter("one",one);
		dataProvider.setParameter("rcoff",rcoff);
		dataProvider.setParameter("xx",xx);
		dataProvider.setParameter("yy",yy);
		dataProvider.setParameter("zz",zz);
		dataProvider.setParameter("hsq2",hsq2);
		dataProvider.setParameter("vaverh",vaverh);
		dataProvider.setParameter("move",move);
		dataProvider.setParameter("etot",etot);
		dataProvider.setParameter("zz",zz);

	}
	
	private void getResults(HashMap<String, Object> results) {

		mdsize 		= (int) results.get("mdsize");
		one 		= (Particle[]) results.get("one");
		side 		= (double) results.get("side");
		rcoff 		= (double) results.get("rcoff");
		a 			= (double) results.get("a");
		sideh 		= (double) results.get("sideh");
		hsq 		= (double) results.get("hsq");
		hsq2 		= (double) results.get("hsq2");
		npartm 		= (int) results.get("npartm");
		rcoffs 		= (double) results.get("rcoffs");
		tscale 		= (double) results.get("tscale");
		vaver 		= (double) results.get("vaver");
		vaverh 		= (double) results.get("vaverh");
		xvelocity 	= (double) results.get("xvelocity");
		yvelocity 	= (double) results.get("yvelocity");
		zvelocity	= (double) results.get("zvelocity");
		ekin 		= (double) results.get("ekin");
		epot		= (double[]) results.get("epot");
		vir 		= (double[]) results.get("vir");
		interacts 	= (int[]) results.get("interacts");
		etot 		= (double) results.get("etot");
		temp 		= (double) results.get("temp");
		pres 		= (double) results.get("pres");
		vel 		= (double) results.get("vel");
		rp 			= (double) results.get("rp");
		sc 			= (double) results.get("sc");
		
		System.out.println("EPOT " + epot);
		
	}

	public JPPFJob createJob_1(final int nbTasks) {

		job = new JPPFJob();
		job.setDataProvider(dataProvider);

		job.setName("Job");

		for (int i = 0; i < nbTasks; i++) {

			// Se crea una Task_1
			SuperTask task = new SuperTask(i);

			tasks.add(task);
			
			try {
				// Se agrega la Task al Job y se le asigna un ID
				job.add(task).setId("Job_Task_Part: [" + i + "]");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

		return job;
	}

	private void updateTaskStates(SuperTask.STATE state){

		// Se cambia el estado de las tareas
		for (SuperTask task : tasks){
			task.setState(state);
		}

		// Se vuelven a mandar los datos actualizados
		populateDataProvider();
		
		job.setDataProvider(dataProvider);
		
	}


	/**
	 * Process the results of a job.
	 * @param job the JPPF job whose results are printed.
	 */
	public void processResults(final JPPFJob job) {
		System.out.printf("*** results for job '%s' ***%n", job.getName());
		List<Task<?>> results = job.getAllResults();
		for (Task<?> task: results) {
			if (task.getThrowable() != null) { // if the task execution raised an exception
				System.out.printf("%s raised an exception : %s%n", task.getId(), /*ExceptionUtils.getMessage(task.getThrowable())*/ ExceptionUtils.getStackTrace(task.getThrowable()).toString());
			} else { // otherwise display the task result
				System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
			}
		}
	}

	public void processResults2(JPPFJob job) {
		System.out.printf("Resultados del Job '%s' %n", job.getName());
		
		List<Task<?>> results = job.getAllResults();
		for (Task<?> task: results) {
			if (task.getThrowable() != null) { // if the task execution raised an exception
				System.out.printf("%s raised an exception : %s%n", task.getId(), /*ExceptionUtils.getMessage(task.getThrowable())*/ ExceptionUtils.getStackTrace(task.getThrowable()).toString());
			} else { // otherwise display the task result
				
				HashMap<String,Object> hashResults = (HashMap<String,Object>) task.getResult();
				System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
				getResults(hashResults);
			}
		}
	}
	
	
	

	private void ensureSufficientConnections(JPPFClient jppfClient, int nbConnections) throws Exception {
		// wait until a connection pool is available
		JPPFConnectionPool pool = jppfClient.awaitActiveConnectionPool();
		// make sure the pool has enough connections and wait until all connections are active
		pool.awaitActiveConnections(Operator.AT_LEAST, 1);
		// alternatively with a single method call: wait until there is a connection pool with at least <nbConnections> active connections, for as long as it takes
		//jppfClient.awaitConnectionPools(Operator.AT_LEAST, nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE);
	}

	public void validateResults(){

		double refval[] = {1731.4306625334357,7397.392307839352};
		double dev = Math.abs(ek[0] - refval[1]);
		if (dev > 1.0e-10 ){
			System.out.println("Falló la validación");
			System.out.println("Kinetic Energy = " + ek[0] + "  " + dev + "  " + 1);

		}else{
			System.out.println("Los resultados estan bien");
		}

	}

}
