package com.cn2;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * @author hyang
 *
 */
public class App {
	
	public static void main(String[] args) {

	//	String springConfig = "spring/batch/jobs/job_dev.xml";
		String springConfig = "spring/batch/jobs/job_prod.xml";

		ApplicationContext context = new ClassPathXmlApplicationContext(springConfig);

	}
}
