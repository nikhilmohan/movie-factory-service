package com.nikhilm.hourglass.moviefactory.services;

import com.nikhilm.hourglass.moviefactory.models.Movie;
import com.nikhilm.hourglass.moviefactory.models.MovieSearchResult;
import com.nikhilm.hourglass.moviefactory.models.MovieSummary;
import com.nikhilm.hourglass.moviefactory.repositories.MovieKeywordRepository;
import com.nikhilm.hourglass.moviefactory.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@Configuration
@PropertySource("classpath:.env")
public class MovieFactoryService {

    @Autowired
    MovieRepository movieRepository;

    @Value("${omdb.uri}")
    String omdbUri;

    @Value("${apiKey}")
    String apiKey;

    @Value("${ratingBenchmark}")
    double benchmarkRating;

    public void generateMovieFeedsFromKeyword(String keyword, int page)   {
        WebClient client = WebClient.create(omdbUri);
        log.info("API KEY" + apiKey);

        Mono<MovieSearchResult> searchResultMono = client.get().uri("?s="+ keyword +"&apikey=" + apiKey + "&type=movie&page=" + page)
                .retrieve()
                .bodyToMono(MovieSearchResult.class).log();

        Flux<MovieSummary> movieSummaryFlux = searchResultMono
                  .flatMapMany(movieSearchResult -> Flux.fromIterable(movieSearchResult.getSearch()));
        Flux<Movie> movieFlux =
                movieSummaryFlux.flatMap( movieSummary -> {
                    //call detailed uri
                    return client.get().uri("?i=" + movieSummary.getImdbID() + "&apikey="+apiKey)
                            .retrieve()
                            .bodyToMono(Movie.class)
                            .onErrorReturn(new Movie())
                            .filter(movie -> Double.parseDouble(movie.getImdbRating()) >= benchmarkRating)
                            .flatMap(movieRepository::save);

                            });

        movieFlux.subscribe();

    }
}
