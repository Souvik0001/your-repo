package com.bfhl.java_qualifier;

import com.bfhl.java_qualifier.dto.GenerateReq;
import com.bfhl.java_qualifier.dto.GenerateRes;
import com.bfhl.java_qualifier.dto.SubmitReq;
import com.bfhl.java_qualifier.sql.SolverFactory;
import com.bfhl.java_qualifier.sql.SqlSolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class JavaQualifierApplication {

	@Value("${app.name}")  String name;
	@Value("${app.regNo}") String regNo;
	@Value("${app.email}") String email;

	@Value("${bfhl.generate}") String generateUrl;
	@Value("${bfhl.submit}")   String submitUrl;

	public static void main(String[] args) {
		SpringApplication.run(JavaQualifierApplication.class, args);
	}

	@Bean WebClient webClient(WebClient.Builder b) { return b.build(); }
	@Bean SolverFactory solverFactory() { return new SolverFactory(); }
	@Bean ObjectMapper om() { return new ObjectMapper(); }

	@Bean
	ApplicationRunner run(WebClient http, SolverFactory factory, ObjectMapper om) {
		return args -> {
			// 1) Generate webhook + token
			GenerateReq body = new GenerateReq(name, regNo, email);
			GenerateRes gen = http.post()
					.uri(generateUrl)
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(body)
					.retrieve()
					.bodyToMono(GenerateRes.class)
					.block();

			if (gen == null || gen.accessToken() == null || gen.webhook() == null) {
				throw new IllegalStateException("Failed to get webhook/token");
			}

			// 2) Pick solver
			SqlSolver solver = factory.forRegNo(regNo);
			String finalSql = solver.solve();

			// 3) Save locally
			Path out = Path.of("final-sql.txt");
			Files.writeString(out, finalSql);

			// 4) Submit to webhook with JWT
			SubmitReq submit = new SubmitReq(finalSql);
			http.post()
					.uri(submitUrl)
					.header(HttpHeaders.AUTHORIZATION, gen.accessToken())
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(submit)
					.retrieve()
					.toBodilessEntity()
					.block();

			System.out.println("✅ Submitted. Final SQL saved to " + out.toAbsolutePath());
		};
	}
}
