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
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;

import jgfutil.JGFInstrumentor;

public class Work {

	// Variables de la clase Work
	private ArrayList<SuperTask> tasks = new ArrayList<SuperTask>();
	private HashMap<String, HashMap<String, Object>> values = new HashMap<String, HashMap<String, Object>>();

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

	private DataProvider dataProvider;

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
					//processResults(event.getJob());
					processResults2(event.getJob());
				}
			});

			// Parte 1 -> Distribuido
			jppfClient.submitJob(job);
			//job.getJobTasks().get(0).run();
			
			
			// Trabajo del thread principal
			jgfutil.JGFInstrumentor.addTimer("Section3:MolDyn:Run");
			jgfutil.JGFInstrumentor.startTimer("Section3:MolDyn:Run");

			// For - Delegado al thread principal

			move = 0;
			for (move = 0; move < movemx; move++) {

				System.out.println("\n Nueva iteración: " + move + "\n");

				// Parte 2 -> Distribuido
				updateTaskStates(SuperTask.STATE.PART_2);
				jppfClient.submitJob(job);
				//job.getJobTasks().get(0).run();

				// Trabajo del thread principal
				for( j = 0; j < 3; j++) {
					for (i = 0; i < mdsize; i++) {
						sh_force[j][i] = 0.0;
					}
				}

				// Parte 3 -> Distribuido
				updateTaskStates(SuperTask.STATE.PART_3);
				jppfClient.submitJob(job);
				//job.getJobTasks().get(0).run();

				// Trabajo del thread principal
				for(int k = 0; k < 3; k++) {
					for(i = 0 ; i < mdsize; i++) {
						for( j = 0; j < n_Task; j++) {
							sh_force[k][i] += sh_force2[k][j][i];
						}
					}
				}

				for(int k = 0; k < 3; k++) {
					for(i = 0; i < mdsize; i++) {
						for( j = 0; j < n_Task; j++) {
							sh_force2[k][j][i] = 0.0;
						}
					}
				}

				for( j = 1; j < n_Task; j++) {
					epot[0] += epot[j];
					vir[0] += vir[j];
				}
				for( j = 1; j < n_Task; j++) {       
					epot[j] = epot[0];
					vir[j] = vir[0];
				}
				for( j = 0; j < n_Task; j++) {
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
				//job.getJobTasks().get(0).run();

			}

			// Trabajo del thread principal
			JGFInstrumentor.stopTimer("Section3:MolDyn:Run");
		}

		System.out.println("Todos los Trabajos finalizaron");

	}

	private void populateDataProvider() {

		if (one != null)
			dataProvider.setParameter("one",one);
		dataProvider.setParameter("PARTSIZE",PARTSIZE);
		dataProvider.setParameter("a", a);
		dataProvider.setParameter("den",den);
		dataProvider.setParameter("ek", ek);
		dataProvider.setParameter("ekin",ekin);
		dataProvider.setParameter("epot", epot);
		dataProvider.setParameter("etot",etot);
		dataProvider.setParameter("hsq",hsq);
		dataProvider.setParameter("hsq2",hsq2);
		dataProvider.setParameter("interacts", interacts);
		dataProvider.setParameter("mdsize",mdsize);
		dataProvider.setParameter("mm",mm);
		dataProvider.setParameter("move",move);
		dataProvider.setParameter("npartm",npartm);
		dataProvider.setParameter("pres",pres);
		dataProvider.setParameter("rcoff",rcoff);
		dataProvider.setParameter("rcoffs",rcoffs);
		dataProvider.setParameter("rp",rp);
		dataProvider.setParameter("sc",sc);
		dataProvider.setParameter("sh_force", sh_force);
		dataProvider.setParameter("sh_force2", sh_force2);
		dataProvider.setParameter("side",side);
		dataProvider.setParameter("sideh",sideh);
		dataProvider.setParameter("temp",temp);
		dataProvider.setParameter("tscale",tscale);
		dataProvider.setParameter("vaver",vaver);
		dataProvider.setParameter("vaverh",vaverh);
		dataProvider.setParameter("vel",vel);
		dataProvider.setParameter("vir", vir);
		dataProvider.setParameter("xvelocity",xvelocity);
		dataProvider.setParameter("xx",xx);
		dataProvider.setParameter("yvelocity",yvelocity);
		dataProvider.setParameter("yy",yy);
		dataProvider.setParameter("zvelocity",zvelocity);
		dataProvider.setParameter("zz",zz);
	}

	private void getResults(HashMap<String, Object> results, String taskID) {

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
		sh_force 	= (double[][]) results.get("sh_force");
		sh_force2 	= (double[][][]) results.get("sh_force2");
		ek 			= (double[]) results.get("ek");
		xx 			= (double) results.get("xx");
		yy 			= (double) results.get("yy");
		zz 			= (double) results.get("zz");
		
		/*HashMap<String, Object> taskValues = values.get(taskID); 
		if (taskValues == null){
			values.put(taskID, taskValues = new HashMap<String,Object>());
		}

		taskValues.put("mdsize", results.get("mdsize"));
		taskValues.put("one", results.get("one"));
		taskValues.put("side", results.get("side"));
		taskValues.put("rcoff", results.get("rcoff"));
		taskValues.put("a", results.get("a"));
		taskValues.put("sideh", results.get("sideh"));
		taskValues.put("hsq", results.get("hsq"));
		taskValues.put("hsq2", results.get("hsq2"));
		taskValues.put("npartm", results.get("npartm"));
		taskValues.put("rcoffs", results.get("rcoffs"));
		taskValues.put("tscale", results.get("tscale"));
		taskValues.put("vaver", results.get("vaver"));
		taskValues.put("vaverh", results.get("vaverh"));
		taskValues.put("xvelocity", results.get("xvelocity"));
		taskValues.put("yvelocity", results.get("yvelocity"));
		taskValues.put("zvelocity", results.get("zvelocity"));
		taskValues.put("ekin", results.get("ekin"));
		taskValues.put("epot", results.get("epot"));
		taskValues.put("vir", results.get("vir"));
		taskValues.put("interacts", results.get("interacts"));
		taskValues.put("etot", results.get("etot"));
		taskValues.put("temp", results.get("temp"));
		taskValues.put("pres", results.get("pres"));
		taskValues.put("vel", results.get("vel"));
		taskValues.put("rp", results.get("rp"));
		taskValues.put("sc", results.get("sc"));
		 */

	}

	public JPPFJob createJob_1(final int nbTasks) {

		job = new JPPFJob();
		job.setDataProvider(dataProvider);

		job.setName("Moldyn_Job");

		for (int i = 0; i < nbTasks; i++) {

			// Se crea una Task
			SuperTask task = new SuperTask(i,nbTasks);

			//
			//task.setDataProvider(dataProvider);
			tasks.add(task);

			try {
				// Se agrega la Task al Job y se le asigna un ID
				job.add(task).setId(""+i);
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

			if (task.getThrowable() != null) { 
				System.out.printf("%s raised an exception : %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()).toString());
			} else { 
				System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
				HashMap<String,Object> hashResults = (HashMap<String,Object>) task.getResult();
				if (hashResults == null)
					System.out.println("resultados del task en Null");
				else
					getResults(hashResults,  task.getId());
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
