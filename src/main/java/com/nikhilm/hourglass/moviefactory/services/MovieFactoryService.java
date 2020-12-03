package com.nikhilm.hourglass.moviefactory.services;

import com.nikhilm.hourglass.moviefactory.models.Movie;
import com.nikhilm.hourglass.moviefactory.models.MovieKeyword;
import com.nikhilm.hourglass.moviefactory.models.MovieSearchResult;
import com.nikhilm.hourglass.moviefactory.models.MovieSummary;
import com.nikhilm.hourglass.moviefactory.repositories.MovieKeywordRepository;
import com.nikhilm.hourglass.moviefactory.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@Configuration
@PropertySource(value = "classpath:.env", ignoreResourceNotFound = true)
public class MovieFactoryService {

    @Autowired
    MovieRepository movieRepository;

    String omdbUri;

    String apiKey;

    double benchmarkRating;

    @Value("${ratingBenchmark}")
    public void setBenchmarkRating(double benchmarkRating)  {
        this.benchmarkRating = benchmarkRating;
    }

    @Value("${apiKey}")
    public void setApiKey(String apiKey)    {
        this.apiKey = apiKey;
    }

    @Value("${omdb.uri}")
    public void setOmdbUri(String omdbUri)    {
        this.omdbUri = omdbUri;
    }

    @Autowired
    MovieKeywordRepository movieKeywordRepository;

    @Autowired
    WebClient webClient;

    private Mono<MovieKeyword> generateMovieFeedsFromKeyword(MovieKeyword movieKeyword)   {
        log.info("Benchmark " + benchmarkRating) ;
        log.info("apikey " + apiKey);
        int page = movieKeyword.getLastPageAccessed() + 1;
        Mono<MovieSearchResult> searchResultMono =
                webClient.get().uri(omdbUri + "?s="+ movieKeyword.getKeyword() +"&apikey="
                        + apiKey + "&type=movie&page=" + page)
                .exchange()
                .flatMap(clientResponse -> {
                    return clientResponse.bodyToMono(MovieSearchResult.class);
                });


        Flux<MovieSummary> movieSummaryFlux = searchResultMono
                  .flatMapMany(movieSearchResult -> {
                      log.info("keyword " + movieKeyword.getKeyword() + " " + movieSearchResult.getSearch());
                      return Flux.fromIterable(movieSearchResult.getSearch());
                  });
        return movieSummaryFlux.flatMap( movieSummary -> {
                //call detailed uri
            return webClient.get().uri(omdbUri + "?i=" + movieSummary.getImdbID() + "&apikey="+apiKey)
                .exchange()
                .flatMap(clientResponse -> {
                    return clientResponse.bodyToMono(Movie.class);
                })
                .onErrorReturn(new Movie())
                .filter(movie -> Double.parseDouble(movie.getImdbRating()) >= benchmarkRating)
                .flatMap(movieRepository::save);

        }).then(Mono.just(movieKeyword))
        .flatMap(movieKeyword1 -> {
            log.info("Movie keyword : " + movieKeyword.getKeyword());
            MovieKeyword updatedMovieKeyword = new MovieKeyword(movieKeyword.getId(),
                    movieKeyword.getKeyword(), movieKeyword.getLastPageAccessed() + 1);
            return movieKeywordRepository.save(updatedMovieKeyword);
        });




    }
    public Flux<MovieKeyword> generateMovieFeeds(int searchSize)	{
        log.info("STARTING..." + searchSize);
        Flux<MovieKeyword> movieKeywordFlux = movieKeywordRepository.findAll(Sort.by(Sort.Direction.ASC,
                "lastPageAccessed")).take(searchSize);


        return movieKeywordFlux.flatMap(movieKeyword -> {
                    log.info("Accessed keyWord " + movieKeyword.getKeyword());
                    return generateMovieFeedsFromKeyword(movieKeyword);
                });
//                .thenMany(movieKeywordFlux)
//                .flatMap(movieKeyword -> {
//                    log.info("Movie keyword : " + movieKeyword.getKeyword());
//                    MovieKeyword updatedMovieKeyword = new MovieKeyword(movieKeyword.getId(),
//                            movieKeyword.getKeyword(), movieKeyword.getLastPageAccessed() + 1);
//                    return movieKeywordRepository.save(updatedMovieKeyword);
//                 });
    }

}
