package montecarlo.Better;
/**************************************************************************
*                                                                         *
*         Java Grande Forum Benchmark Suite - Thread Version 1.0          *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/


import jgfutil.*;

public class JGFMonteCarloBenchSizeB3_Better{ 

  public static int nthreads;

  public static void main(String argv[]){

  if(argv.length != 0 ) {
    nthreads = Integer.parseInt(argv[0]);
  } else {
	nthreads = 4;
    System.out.println("The no of threads has not been specified, defaulting to " + nthreads);
    System.out.println("  ");
  }

    JGFInstrumentor.printHeader(3,1,nthreads);

    JGFMonteCarloBench mc = new JGFMonteCarloBench(nthreads); 
    mc.JGFrun(1);
 
  }
}


