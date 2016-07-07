package brian;

import java.util.HashMap;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.node.protocol.DataProvider;

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

	public SuperTask(int id, int n_Task) {
		this.id = id;
		this.n_Task = n_Task;
	}

	// Getters and Setters

	public void setState(STATE state) {
		this.state = state;
	}

	// Methods

	// Paso 1. Sacar Part 1 al thread principal
	
	/*
	private void part_1 (){

		// Parameter determination

		mdsize = PARTSIZE;

		one = new Particle [mdsize];
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

		// Particle Generation

		xvelocity = 0.0;
		yvelocity = 0.0;
		zvelocity = 0.0;

		ijk = 0;
		for (lg = 0; lg <= 1; lg++) {
			for (i = 0; i < mm; i++) {
				for ( j = 0; j < mm; j++) {
					for (k = 0; k < mm; k++) {
						//one[ijk] = new Particle((i*a+lg*a*0.5),(j*a+lg*a*0.5),(k*a),
						//xvelocity,yvelocity,zvelocity,sh_force,sh_force2,id,this);
						one[ijk] = new Particle((i*a+lg*a*0.5),(j*a+lg*a*0.5),(k*a),
								xvelocity,yvelocity,zvelocity,sh_force,id,this);
						ijk = ijk + 1;
					}
				}
			}
		}
		for (lg = 1; lg <= 2; lg++) {
			for (i = 0; i < mm; i++) {
				for (j = 0; j < mm; j++) {
					for (k = 0; k < mm; k++) {
						//one[ijk] = new Particle((i*a+(2-lg)*a*0.5),(j*a+(lg-1)*a*0.5),
						//(k*a+a*0.5),xvelocity,yvelocity,zvelocity,sh_force,sh_force2,id,this);
						one[ijk] = new Particle((i*a+(2-lg)*a*0.5),(j*a+(lg-1)*a*0.5),
								(k*a+a*0.5),xvelocity,yvelocity,zvelocity,sh_force,id,this);
						ijk = ijk + 1;
					}
				}
			}
		}

		// Initialise velocities

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

		// velocity scaling

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
	*/

	private void part_2() {

		/* move the particles and update velocities */

		for (i = 0; i < mdsize; i++) {
			one[i].domove(side,i,sh_force);       
		}

	}

	private void part_3() {

		//Estas 3 variables eran de md_Better
		epot[id] = 0.0;
		vir[id] = 0.0;
		interacts[id] = 0;	

		/* compute forces */

		for (i = 0 + id; i < mdsize; i += n_Task) {
			one[i].force(side,rcoff,mdsize,i,xx,yy,zz,epot,vir,interacts,sh_force2,id,one); 
		}

	}

	private void part_4() {

		/*scale forces, update velocities */

		sum = 0.0;
		for (i = 0; i < mdsize; i++) {
			sum = sum + one[i].mkekin(hsq2,i,sh_force);  
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

	private void setData(DataProvider dataProvider) {

		sh_force2	= dataProvider.getParameter("sh_force2");
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
		one 		= dataProvider.getParameter("one_"+id);
		rcoff 		= dataProvider.getParameter("rcoff");
		sh_force	= dataProvider.getParameter("sh_force");
		side 		= dataProvider.getParameter("side");
		vaverh 		= dataProvider.getParameter("vaverh");
		vir			= dataProvider.getParameter("vir");
		xx 			= dataProvider.getParameter("xx");
		yy 			= dataProvider.getParameter("yy");
		zz			= dataProvider.getParameter("zz");
		tscale		= dataProvider.getParameter("tscale");
		a			= dataProvider.getParameter("a");
		sideh 		= dataProvider.getParameter("sideh");
		npartm 		= dataProvider.getParameter("npartm");
		rcoffs 		= dataProvider.getParameter("rcoffs");
		vaver 		= dataProvider.getParameter("vaver");
		ekin 		= dataProvider.getParameter("ekin");
		vir 		= dataProvider.getParameter("vir");
		temp 		= dataProvider.getParameter("temp");
		pres 		= dataProvider.getParameter("pres");
		vel			= dataProvider.getParameter("vel");
		rp			= dataProvider.getParameter("rp");
		sc			= dataProvider.getParameter("sc");
	}

	@Override
	public void run() {

		DataProvider dataProvider = getDataProvider();

		setData(dataProvider);

		switch(this.state){
		case PART_1: {
			//part_1();
			break;
		}
		case PART_2: {
			part_2();
			break;
		}
		case PART_3: {
			part_3();
			break;
		}
		case PART_4: {
			part_4();
			break;
		}
		default: {
			break;
		}
		}

		// Los resultados los devuelvo en un HashMap

		HashMap<String, Object> result = new HashMap<String, Object>();
		
		result.put("sh_force2_[0]_"+id,sh_force2[0][id]);
		result.put("sh_force2_[1]_"+id,sh_force2[1][id]);
		result.put("sh_force2_[2]_"+id,sh_force2[2][id]);
		result.put("mdsize", mdsize);
		result.put("one_"+id, one);
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
		result.put("ekin",ekin);
		result.put("epot_"+id,epot[id]);
		result.put("vir_"+id,vir[id]);
		result.put("interacts_"+id,interacts[id]);
		result.put("etot",etot);
		result.put("temp",temp);
		result.put("pres",pres);
		result.put("vel",vel);
		result.put("rp",rp);
		result.put("sc",sc);
		result.put("ek_"+id, ek[id]);
		result.put("sh_force",sh_force);
		result.put("PARTSIZE",PARTSIZE);
		result.put("xx",xx);
		result.put("yy",yy);
		result.put("zz",zz);

		// Se devuelven los resultados
		setResult(result);
	}

}
