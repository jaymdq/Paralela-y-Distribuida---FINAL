package brian;

import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;

import moldyn.Better.JGFMolDynBench_Better;
import moldyn.Better.md_Better;

public class SuperTask extends AbstractTask<HashMap<String, Object>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static enum STATE {
	    PART_1, PART_2, PART_3, PART_4 
	}

	// Variables

	protected static final int ITERS = 100;
	protected static final double LENGTH = 50e-10;
	protected static final double m = 4.0026;
	protected static final double mu = 1.66056e-27;
	protected static final double kb = 1.38066e-23;
	protected static final double TSIM = 50;
	protected static final double deltat = 5e-16;

	protected static int PARTSIZE;

	protected static double [] epot;
	protected static double [] vir;
	protected static double [] ek;

	protected int size,mm;
	protected int datasizes[] = {8,13};

	protected static int interactions = 0;
	protected static int [] interacts;

	double count = 0.0;

	// Del Runnable
	protected int i,j,k,lg,mdsize,move;

	protected double l,rcoff,rcoffs,side,sideh,hsq,hsq2,vel,velt;
	protected double a,r,sum,tscale,sc,ekin,ts,sp;
	protected double den = 0.83134;
	protected double tref = 0.722;
	protected double h = 0.064;
	protected double vaver,vaverh,rand;
	protected double etot,temp,pres,rp;
	protected double u1,u2,v1,v2,s, xx, yy, zz;
	protected double xvelocity, yvelocity, zvelocity;

	protected double [][] sh_force;
	protected double [][][] sh_force2;

	protected int ijk,npartm,iseed,tint;
	protected int irep = 10;
	protected int istop = 19;
	protected int iprint = 10;
	protected int movemx = 50;

	protected Random randnum;
	protected Particle one [] = null;

	// Agregadas
	protected int id;
	private STATE state = STATE.PART_1;
	private int n_Task;

	// Constructors

	public SuperTask(int id) {
		this.id = id;
	}

	// Getters and Setters

	public void setState(STATE state) {
		this.state = state;
	}

	// Methods

	private void part_1 (){

		/* Parameter determination */

		//mdsize = md_Better.PARTSIZE;
		mdsize = PARTSIZE;
		one = new Particle [mdsize];
		//l = md_Better.LENGTH;
		l = LENGTH;

		side = Math.pow((mdsize/den),0.3333333);
		rcoff = mm/4.0;

		a = side/mm;
		sideh = side*0.5;
		hsq = h*h;
		hsq2 = hsq*0.5;
		npartm = mdsize - 1;
		rcoffs = rcoff * rcoff;
		tscale = 16.0 / (1.0 * mdsize - 1.0);
		vaver = 1.13 * Math.sqrt(tref / 24.0);
		vaverh = vaver * h;

		/* Particle Generation */

		xvelocity = 0.0;
		yvelocity = 0.0;
		zvelocity = 0.0;
		
		ijk = 0;
		for (lg = 0; lg <= 1; lg++) {
			for (i = 0; i < mm; i++) {
				for ( j = 0; j < mm; j++) {
					for (k = 0; k < mm; k++) {
						one[ijk] = new Particle((i*a+lg*a*0.5),(j*a+lg*a*0.5),(k*a),
								xvelocity,yvelocity,zvelocity,sh_force,sh_force2,id,this);
						ijk = ijk + 1;
					}
				}
			}
		}
		for (lg = 1; lg <= 2; lg++) {
			for (i = 0; i < mm; i++) {
				for (j = 0; j < mm; j++) {
					for (k = 0; k < mm; k++) {
						one[ijk] = new Particle((i*a+(2-lg)*a*0.5),(j*a+(lg-1)*a*0.5),
								(k*a+a*0.5),xvelocity,yvelocity,zvelocity,sh_force,sh_force2,id,this);
						ijk = ijk + 1;
					}
				}
			}
		}
				
		/* Initialise velocities */

		iseed = 0;
		v1 = 0.0;
		v2 = 0.0;

		randnum = new Random(iseed,v1,v2);

		for (i = 0; i < mdsize; i += 2) {
			r  = randnum.seed();
			one[i].xvelocity = r * randnum.v1;
			one[i+1].xvelocity  = r * randnum.v2;
		}

		for (i = 0; i < mdsize; i += 2) {
			r  = randnum.seed();
			one[i].yvelocity = r * randnum.v1;
			one[i+1].yvelocity  = r * randnum.v2;
		}

		for (i = 0; i < mdsize; i += 2) {
			r  = randnum.seed();
			one[i].zvelocity = r * randnum.v1;
			one[i+1].zvelocity  = r * randnum.v2;
		}
		
		/* velocity scaling */

		ekin = 0.0;
		sp = 0.0;

		for(i = 0; i < mdsize; i++) {
			sp = sp + one[i].xvelocity;
		}
		sp = sp / mdsize;

		for(i = 0; i < mdsize; i++) {
			one[i].xvelocity = one[i].xvelocity - sp;
			ekin = ekin + one[i].xvelocity * one[i].xvelocity;
		}
		
		sp = 0.0;
		for(i = 0; i < mdsize; i++) {
			sp = sp + one[i].yvelocity;
		}
		sp = sp / mdsize;

		for(i = 0; i < mdsize; i++) {
			one[i].yvelocity = one[i].yvelocity - sp;
			ekin = ekin + one[i].yvelocity * one[i].yvelocity;
		}

		sp = 0.0;
		for(i = 0; i < mdsize; i++) {
			sp = sp + one[i].zvelocity;
		}
		sp = sp / mdsize;

		for(i = 0; i < mdsize; i++) {
			one[i].zvelocity = one[i].zvelocity - sp;
			ekin = ekin + one[i].zvelocity*one[i].zvelocity;
		}
				
		ts = tscale * ekin;
		sc = h * Math.sqrt(tref/ts);

		for(i = 0; i < mdsize; i++) {

			one[i].xvelocity = one[i].xvelocity * sc;     
			one[i].yvelocity = one[i].yvelocity * sc;     
			one[i].zvelocity = one[i].zvelocity * sc;     

		}
		
	}

	private void part_2() {

		/* move the particles and update velocities */

		for (i = 0; i < mdsize; i++) {
			one[i].domove(side,i);       
		}

	}

	private void part_3() {

		//Estas 3 variables eran de md_Better
		epot[id] = 0.0;
		vir[id] = 0.0;
		interacts[id] = 0;	

		/* compute forces */

		for (i = 0 + id; i < mdsize; i +=/*JGFMolDynBench_Better.nthreads*/ n_Task) {
			if (i > 0)
				return;
			//one[i].force(side,rcoff,mdsize,i,xx,yy,zz);
			//one[i].force(side,rcoff,mdsize,i,xx,yy,zz,epot,vir,interacts); 
		}

	}

	private void part_4() {

		/*scale forces, update velocities */

		sum = 0.0;
		for (i = 0; i < mdsize; i++) {
			sum = sum + one[i].mkekin(hsq2,i);  
		}

		ekin = sum/hsq;

		vel = 0.0;
		count = 0.0;

		/* average velocity */

		for (i = 0; i < mdsize; i++) {
			velt = one[i].velavg(vaverh,h);
			if (velt > vaverh) { count = count + 1.0; }
			vel = vel + velt;                    
		}

		vel = vel / h;

		/* temperature scale if required */

		if((move < istop) && (((move+1) % irep) == 0)) {
			sc = Math.sqrt(tref / (tscale*ekin));
			for (i=0;i<mdsize;i++) {
				one[i].dscal(sc,1);
			}
			ekin = tref / tscale;
		}

		/* sum to get full potential energy and virial */

		//Estas 3 variables eran de md_Better
		//ek
		//epot
		//vir

		if(((move+1) % iprint) == 0) {
			ek[id] = 24.0*ekin;
			epot[id] = 4.0 * epot[id];
			etot = ek[id] + epot[id];
			temp = tscale * ekin;
			pres = den * 16.0 * (ekin - vir[id]) / mdsize;
			vel = vel / mdsize; 
			rp = (count / mdsize) * 100.0;
		}

	}


	private void setData_1(DataProvider dataProvider){

		PARTSIZE	= dataProvider.getParameter("PARTSIZE");
		mm 			= dataProvider.getParameter("mm");
		side 		= dataProvider.getParameter("side");
		hsq			= dataProvider.getParameter("hsq");
		sh_force	= dataProvider.getParameter("sh_force");
		sh_force2	= dataProvider.getParameter("sh_force2");
		epot 		= dataProvider.getParameter("epot");
		vir 		= dataProvider.getParameter("vir");
		ek 			= dataProvider.getParameter("ek");
		interacts	= dataProvider.getParameter("interacts");
		
	}

	private void setData_2(DataProvider dataProvider){
		
		mdsize	= dataProvider.getParameter("mdsize");
		one 	= dataProvider.getParameter("one");
		side 	= dataProvider.getParameter("side");
		
	}

	private void setData_3(DataProvider dataProvider){
	
		side 		= dataProvider.getParameter("side");
		rcoff 		= dataProvider.getParameter("rcoff");
		mdsize		= dataProvider.getParameter("mdsize");
		xx 			= dataProvider.getParameter("xx");
		yy 			= dataProvider.getParameter("yy");
		zz			= dataProvider.getParameter("zz");
		epot 		= dataProvider.getParameter("epot");
		vir 		= dataProvider.getParameter("vir");
		interacts	= dataProvider.getParameter("interacts");
		one 	= dataProvider.getParameter("one");
		
	}

	private void setData_4(DataProvider dataProvider){

		one		= dataProvider.getParameter("one");
		hsq2	= dataProvider.getParameter("hsq2");
		hsq		= dataProvider.getParameter("hsq");
		vaverh 	= dataProvider.getParameter("vaverh");
		move 	= dataProvider.getParameter("move");
		ek 		= dataProvider.getParameter("ek");
		epot 	= dataProvider.getParameter("epot");
		etot 	= dataProvider.getParameter("etot");
		den 	= dataProvider.getParameter("den");
		vir		= dataProvider.getParameter("vir");
		
	}

	private void setData(DataProvider dataProvider) {
		PARTSIZE	= dataProvider.getParameter("PARTSIZE");
		den 		= dataProvider.getParameter("den");
		ek 			= dataProvider.getParameter("ek");
		epot 		= dataProvider.getParameter("epot");
		etot	 	= dataProvider.getParameter("etot");
		hsq			= dataProvider.getParameter("hsq");
		hsq2		= dataProvider.getParameter("hsq2");
		interacts	= dataProvider.getParameter("interacts");
		mdsize		= dataProvider.getParameter("mdsize");
		mm 			= dataProvider.getParameter("mm");
		move 		= dataProvider.getParameter("move");
		one 		= dataProvider.getParameter("one");
		rcoff 		= dataProvider.getParameter("rcoff");
		sh_force	= dataProvider.getParameter("sh_force");
		sh_force2	= dataProvider.getParameter("sh_force2");
		side 		= dataProvider.getParameter("side");
		vaverh 		= dataProvider.getParameter("vaverh");
		vir			= dataProvider.getParameter("vir");
		xx 			= dataProvider.getParameter("xx");
		yy 			= dataProvider.getParameter("yy");
		zz			= dataProvider.getParameter("zz");		
		
	}

	
	@Override
	public void run() {

		DataProvider dataProvider = getDataProvider();

		if (dataProvider.getParameter("epot") == null){
			setResult(null);
			return;
		}		
		
		setData(dataProvider);
		
		switch(this.state){
		case PART_1: {
			//setData_1(dataProvider);
			part_1();
		}
		case PART_2: {
			//setData_2(dataProvider);
			part_2();
			break;
		}
		case PART_3: {
			//setData_3(dataProvider);
			part_3();
			break;
		}
		case PART_4: {
			//setData_4(dataProvider);
			part_4();
			break;
		}
		default: {
			break;
		}
		}

		// Los resultados los devuelvo en un HashMap

		HashMap<String, Object> result = new HashMap<String, Object>();
		
		result.put("mdsize", mdsize);
		result.put("one", one);
		result.put("side", side);
		result.put("rcoff", rcoff);
		result.put("a", a);
		result.put("sideh",sideh);
		result.put("hsq",hsq);
		result.put("hsq2",hsq2);
		result.put("npartm",npartm);
		result.put("rcoffs",rcoffs);
		result.put("tscale",tscale);
		result.put("vaver",vaver);
		result.put("vaverh",vaverh);
		result.put("xvelocity",xvelocity);
		result.put("yvelocity",yvelocity);
		result.put("zvelocity",zvelocity);
		result.put("ekin",ekin);
		result.put("epot",epot);
		result.put("vir",vir);
		result.put("interacts",interacts);
		result.put("etot",etot);
		result.put("temp",temp);
		result.put("pres",pres);
		result.put("vel",vel);
		result.put("rp",rp);
		result.put("sc",sc);
	
		// Se devuelven los resultados
		setResult(result);
	}

}
