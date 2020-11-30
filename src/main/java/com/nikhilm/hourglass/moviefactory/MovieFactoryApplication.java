package com.nikhilm.hourglass.moviefactory;

import com.nikhilm.hourglass.moviefactory.models.Movie;
import com.nikhilm.hourglass.moviefactory.models.MovieKeyword;
import com.nikhilm.hourglass.moviefactory.repositories.MovieKeywordRepository;
import com.nikhilm.hourglass.moviefactory.services.MovieFactoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Sort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@Slf4j
public class MovieFactoryApplication implements CommandLineRunner
{
	@Autowired
	MovieFactoryService movieFactoryService;

	@Value("${searchSize}")
	private int searchSize;

	public static void main(String[] args) {
		SpringApplication.run(MovieFactoryApplication.class, args);
	}


	@Override
	public void run(String... args) throws Exception {
		movieFactoryService.generateMovieFeeds(searchSize).blockLast();
	}

	@Bean
	WebClient webClient()	{
		return WebClient.create();
	}
}
