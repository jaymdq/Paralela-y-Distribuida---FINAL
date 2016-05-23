package org.jppf.example.concurrentjobs;

import java.util.concurrent.Callable;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;

/**
 * Submits a job and returns it after it completes.
 */
public class MyCallable implements Callable<JPPFJob> {
	/**
	 * The JPPF client which submits the job.
	 */
	private final JPPFClient jppfClient;
	/**
	 * The job to submit.
	 */
	private final JPPFJob job;

	/**
	 * Initialize this callable.
	 * 
	 * @param jppfClient
	 *            the JPPF client which submits the job.
	 * @param job
	 *            the job to submit.
	 */
	public MyCallable(final JPPFClient jppfClient, final JPPFJob job) {
		this.jppfClient = jppfClient;
		this.job = job;
	}

	@Override
	public JPPFJob call() throws Exception {
		// submit the job, blocking until the job completes
		jppfClient.submitJob(job);
		// return the job once completed
		return job;
	}
}
