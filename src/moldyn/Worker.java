package moldyn;

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

public class Worker {

	// Variables de la clase Work
	private ArrayList<SuperTask> tasks;
	private HashMap<String, Particle[]> oneValues;

	// Variables relacionadas al problema

	private static double den = 0.83134;
	private static int interactions = 0;
	private static double [] epot;
	private static double [] vir;
	private static double [] ek;
	private static int [] interacts;
	private static int mm = 13;
	private static int PARTSIZE = mm*mm*mm*4;
	private int i,j,mdsize,move;
	private double rcoff,rcoffs,side,sideh,hsq,hsq2,vel;
	private double a,tscale,sc,ekin;
	private double vaver,vaverh;
	private double etot,temp,pres,rp;
	private double xx, yy, zz;
	private double [][] sh_force;
	private double [][][] sh_force2;
	private int npartm;
	private static int movemx = 50;
	private int n_Task;
	private DataProvider dataProvider;
	private JPPFJob job;


	// Constructors

	public Worker(int n_Task) {

		this.n_Task = n_Task;
		this.tasks = new ArrayList<SuperTask>();
		this.oneValues = new HashMap<String, Particle[]>();

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

			// Difusi�n de los datos 
			// Aclaraci�n: el dataProvider tiene que ser creado y rellenado antes de crear el Job.
			dataProvider = new MemoryMapDataProvider();
			populateDataProvider();

			// Se crea el trabajo 1
			JPPFJob job = createJob(n_Task);

			// Determinamos que el Job sea bloqueante
			job.setBlocking(true);

			job.addJobListener(new JobListenerAdapter() {
				@Override
				public synchronized void jobEnded(JobEvent event) {
					//processResults(event.getJob());
					processResults(event.getJob());
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

				//System.out.println("\n Nueva iteraci�n: " + move );

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

		JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Run", (double) interactions);
	    JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Total", 1);

	}

	private void populateDataProvider() {

		for (String key : oneValues.keySet()){
			dataProvider.setParameter(key, oneValues.get(key));
		}

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
		dataProvider.setParameter("xx",xx);
		dataProvider.setParameter("yy",yy);
		dataProvider.setParameter("zz",zz);
	}

	private void getResults(Task<?> task) {

		@SuppressWarnings("unchecked")
		HashMap<String, Object> results = (HashMap<String, Object>) task.getResult();

		mdsize 			= (int) results.get("mdsize");
		side 			= (double) results.get("side");
		rcoff 			= (double) results.get("rcoff");
		a 				= (double) results.get("a");
		sideh 			= (double) results.get("sideh");
		hsq 			= (double) results.get("hsq");
		hsq2 			= (double) results.get("hsq2");
		npartm 			= (int) results.get("npartm");
		rcoffs 			= (double) results.get("rcoffs");
		tscale 			= (double) results.get("tscale");
		vaver 			= (double) results.get("vaver");
		vaverh 			= (double) results.get("vaverh");
		ekin 			= (double) results.get("ekin");
		etot 			= (double) results.get("etot");
		temp 			= (double) results.get("temp");
		pres 			= (double) results.get("pres");
		vel 			= (double) results.get("vel");
		rp 				= (double) results.get("rp");
		sc 				= (double) results.get("sc");
		sh_force 		= (double[][]) results.get("sh_force");
		xx 				= (double) results.get("xx");
		yy 				= (double) results.get("yy");
		zz 				= (double) results.get("zz");
		
		// Utilizaci�n del id de la Task
		Integer id 	= Integer.parseInt(task.getId());	
	
		oneValues.put("one_"+task.getId(), (Particle[]) results.get("one_"+id));
		epot[id]		= (double) results.get("epot_"+id);
		vir[id]			= (double) results.get("vir_"+id);
		interacts[id]	= (int) results.get("interacts_"+id);
		ek[id]			= (double) results.get("ek_"+id);
			
		sh_force2[0][id] =  (double[]) results.get("sh_force2_[0]_"+id);
		sh_force2[1][id] =  (double[]) results.get("sh_force2_[1]_"+id);
		sh_force2[2][id] =  (double[]) results.get("sh_force2_[2]_"+id);

	}

	public JPPFJob createJob(final int nbTasks) {

		job = new JPPFJob();
		job.setDataProvider(dataProvider);

		job.setName("Moldyn_Job");

		for (int i = 0; i < nbTasks; i++) {

			// Se crea una Task
			SuperTask task = new SuperTask(i,nbTasks);

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

	public void processResults(JPPFJob job) {
		//System.out.printf("Resultados del Job '%s' %n", job.getName());

		List<Task<?>> results = job.getAllResults();

		for (Task<?> task: results) {

			if (task.getThrowable() != null) { 
				System.out.printf("%s raised an exception : %s%n", task.getId(), ExceptionUtils.getStackTrace(task.getThrowable()).toString());
			} else { 
				//System.out.printf("result of %s : %s%n", task.getId(), task.getResult());
				getResults(task);
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
			System.out.println("\n\nFall� la validaci�n");
			System.out.println("Kinetic Energy = " + ek[0] + "  " + dev + "  " + 1);

		}else{
			System.out.println("\n\nLos resultados estan bien\n");
		}

	}

}
