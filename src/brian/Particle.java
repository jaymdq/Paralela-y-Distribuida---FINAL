package brian;

import java.io.Serializable;

public class Particle implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public double xcoord, ycoord, zcoord;
	public double xvelocity,yvelocity,zvelocity;
	int part_id;
	int id;
	double [][] sh_force;
	SuperTask runner;

	public Particle(double xcoord, double ycoord, double zcoord, double xvelocity,
			double yvelocity,double zvelocity,double [][] sh_force,int id, SuperTask runner) {

		this.xcoord = xcoord; 
		this.ycoord = ycoord; 
		this.zcoord = zcoord;
		this.xvelocity = xvelocity;
		this.yvelocity = yvelocity;
		this.zvelocity = zvelocity;
		this.sh_force = sh_force;
		this.id=id;
		this.runner=runner;
	}

	public void domove(double side,int part_id, double [][] sh_force) {

		xcoord = xcoord + xvelocity + sh_force[0][part_id];
		ycoord = ycoord + yvelocity + sh_force[1][part_id];
		zcoord = zcoord + zvelocity + sh_force[2][part_id];

		if(xcoord < 0) { xcoord = xcoord + side; } 
		if(xcoord > side) { xcoord = xcoord - side; }
		if(ycoord < 0) { ycoord = ycoord + side; }
		if(ycoord > side) { ycoord = ycoord - side; }
		if(zcoord < 0) { zcoord = zcoord + side; }
		if(zcoord > side) { zcoord = zcoord - side; }

		xvelocity = xvelocity + sh_force[0][part_id];
		yvelocity = yvelocity + sh_force[1][part_id];
		zvelocity = zvelocity + sh_force[2][part_id];

	}

	public void force(double side, double rcoff,int mdsize,int x, double xx, double yy, double zz,double [] epot,double [] vir,int[] interacts, double[][][] sh_force2) {

		double sideh;
		double rcoffs;

		double fxi,fyi,fzi;
		double rd,rrd,rrd2,rrd3,rrd4,rrd6,rrd7,r148;
		double forcex,forcey,forcez;

		sideh = 0.5*side; 
		rcoffs = rcoff*rcoff;

		fxi = 0.0;
		fyi = 0.0;
		fzi = 0.0;
	
		for (int i = x + 1; i < mdsize; i++) {
			xx = this.xcoord - runner.one[i].xcoord;
			yy = this.ycoord - runner.one[i].ycoord;
			zz = this.zcoord - runner.one[i].zcoord;

			if(xx < (-sideh)) { xx = xx + side; }
			if(xx > (sideh))  { xx = xx - side; }
			if(yy < (-sideh)) { yy = yy + side; }
			if(yy > (sideh))  { yy = yy - side; }
			if(zz < (-sideh)) { zz = zz + side; }
			if(zz > (sideh))  { zz = zz - side; }


			rd = xx*xx + yy*yy + zz*zz;

			if(rd <= rcoffs) {
				rrd = 1.0/rd;
				rrd2 = rrd*rrd;
				rrd3 = rrd2*rrd;
				rrd4 = rrd2*rrd2;
				rrd6 = rrd2*rrd4;
				rrd7 = rrd6*rrd;
				//md_Better.epot[id] = md_Better.epot[id] + (rrd6 - rrd3);
				epot[id] = epot[id] + (rrd6 - rrd3);
				r148 = rrd7 - 0.5*rrd4;
				//md_Better.vir[id] = md_Better.vir[id] - rd*r148;
				vir[id] = vir[id] - rd*r148;
				forcex = xx * r148;
				fxi = fxi + forcex;

				sh_force2[0][id][i] = sh_force2[0][id][i] - forcex;

				forcey = yy * r148;
				fyi = fyi + forcey;

				sh_force2[1][id][i] = sh_force2[1][id][i] - forcey;

				forcez = zz * r148;
				fzi = fzi + forcez;

				sh_force2[2][id][i] = sh_force2[2][id][i] - forcez;

				//md_Better.interacts[id]++;
				interacts[id]++;
			}

		}

		sh_force2[0][id][x] = sh_force2[0][id][x] + fxi;
		sh_force2[1][id][x] = sh_force2[1][id][x] + fyi;
		sh_force2[2][id][x] = sh_force2[2][id][x] + fzi;

	}

	public double mkekin(double hsq2,int part_id, double[][] sh_force) {

		double sumt = 0.0; 

		xvelocity = xvelocity + sh_force[0][part_id]; 
		yvelocity = yvelocity + sh_force[1][part_id]; 
		zvelocity = zvelocity + sh_force[2][part_id]; 

		sumt = (xvelocity*xvelocity)+(yvelocity*yvelocity)+(zvelocity*zvelocity);
		return sumt;
	}

	public double velavg(double vaverh,double h) {

		double velt;
		double sq;

		sq = Math.sqrt(xvelocity*xvelocity + yvelocity*yvelocity +
				zvelocity*zvelocity);

		velt = sq;
		return velt;
	}

	public void dscal(double sc,int incx) {

		xvelocity = xvelocity * sc;
		yvelocity = yvelocity * sc;   
		zvelocity = zvelocity * sc;   

	}

}