package br.edu.faculdadevincit.crm_vincit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrmVincitApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmVincitApplication.class, args);
	}

}
