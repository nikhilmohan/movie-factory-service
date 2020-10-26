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
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;

@SpringBootApplication
@Slf4j
public class MovieFactoryApplication implements CommandLineRunner
{
	@Autowired
	MovieFactoryService movieFactoryService;

	@Autowired
	MovieKeywordRepository movieKeywordRepository;

	@Value("${searchSize}")
	private int searchSize;

	public static void main(String[] args) {
		SpringApplication.run(MovieFactoryApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Starting...");
		Flux<MovieKeyword> movieKeywordFlux = movieKeywordRepository.findAll(Sort.by(Sort.Direction.ASC,
				"lastPageAccessed")).take(searchSize);

		movieKeywordFlux.flatMap(movieKeyword -> {
			log.info("Accessed keyWord " + movieKeyword.getKeyword());
			movieFactoryService.generateMovieFeedsFromKeyword(movieKeyword.getKeyword(),
					movieKeyword.getLastPageAccessed() + 1);

			MovieKeyword updatedMovieKeyword = new MovieKeyword(movieKeyword.getId(),
					movieKeyword.getKeyword(), movieKeyword.getLastPageAccessed() + 1);
			return movieKeywordRepository.save(updatedMovieKeyword);
		}).subscribe();

	}
}
