package com.nikhilm.hourglass.moviefactory.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikhilm.hourglass.moviefactory.models.Movie;
import com.nikhilm.hourglass.moviefactory.models.MovieKeyword;
import com.nikhilm.hourglass.moviefactory.models.MovieSearchResult;
import com.nikhilm.hourglass.moviefactory.models.MovieSummary;
import com.nikhilm.hourglass.moviefactory.repositories.MovieKeywordRepository;
import com.nikhilm.hourglass.moviefactory.repositories.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieFactoryServiceTest {

    @Mock
    MovieKeywordRepository movieKeywordRepository;

    @InjectMocks
    MovieFactoryService movieFactoryService;

    @Mock
    WebClient webClient;

    @Mock
    MovieRepository movieRepository;

    Map<String, MovieSearchResult> movieSearchResultMap = new HashMap<>();

    @BeforeEach
    public void setup() {

        MovieSearchResult forestMovieSearchResult = new MovieSearchResult();
        MovieSearchResult carMovieSearchResult = new MovieSearchResult();
        MovieSearchResult landMovieSearchResult = new MovieSearchResult();

        List<MovieSummary> forestMovieSummaries =
                Arrays.asList(new MovieSummary("forest movie", "2009", "abc", "/forest.jpg"),
                        new MovieSummary("forest movie1", "2015", "def", "/car.jpg"),
                        new MovieSummary("forest movie 2", "2000", "xyz", "land.jpg"));

        forestMovieSearchResult.setSearch(forestMovieSummaries);


        List<MovieSummary> carMovieSummaries =
                Arrays.asList(new MovieSummary("car movie", "2009", "abc", "/forest.jpg"),
                        new MovieSummary("car movie 1", "2015", "def", "/car.jpg"),
                        new MovieSummary("car 2", "2000", "xyz", "land.jpg"));


        List<MovieSummary> landMovieSummaries =
                Arrays.asList(new MovieSummary("land movie", "2009", "abc", "/forest.jpg"),
                        new MovieSummary("la la land", "2015", "def", "/car.jpg"),
                        new MovieSummary("no mans land", "2000", "xyz", "land.jpg"));

        carMovieSearchResult.setSearch(carMovieSummaries);
        landMovieSearchResult.setSearch(landMovieSummaries);
        movieSearchResultMap.put("forest", forestMovieSearchResult);
        movieSearchResultMap.put("car", carMovieSearchResult);
        movieSearchResultMap.put("land", landMovieSearchResult);



    }

    private ClientResponse getClientResponse(String keyword)    {
        ObjectMapper objectMapper = new ObjectMapper();
        String body = null;
        try {
            body = objectMapper.writeValueAsString(movieSearchResultMap.get(keyword));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body).build();
    }


    @Test
    public void testGenerateFeeds() {



        movieFactoryService.setApiKey("12345");
        movieFactoryService.setBenchmarkRating(0);
        movieFactoryService.setOmdbUri("http://omdb/");


        Movie movie = new Movie();


        ObjectMapper objectMapper1 = new ObjectMapper();
        String movieResponse = null;
        try {
            movieResponse = objectMapper1.writeValueAsString(movie);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        ClientResponse movieClientResponse = ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(movieResponse).build();

        List<MovieKeyword> keywordList = Arrays.asList(new MovieKeyword("1", "forest", 1),
        new MovieKeyword("2", "land", 2), new MovieKeyword("3", "car", 3));

        Flux<MovieKeyword> movieKeywordFlux = Flux.fromIterable(keywordList);
        Mockito.when(movieKeywordRepository.findAll(Sort.by(Sort.Direction.ASC, "lastPageAccessed"))).thenReturn(movieKeywordFlux);

        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock
                = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecForestMock = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecCarMock = mock(WebClient.RequestHeadersSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecLandMock = mock(WebClient.RequestHeadersSpec.class);

        WebClient.RequestHeadersSpec requestHeadersSpecMovieMock = mock(WebClient.RequestHeadersSpec.class);
        when(webClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri(contains("forest")))
                .thenReturn(requestHeadersSpecForestMock);
        when(requestHeadersUriSpecMock.uri(contains("car")))
                .thenReturn(requestHeadersSpecCarMock);
        when(requestHeadersUriSpecMock.uri(contains("land")))
                .thenReturn(requestHeadersSpecLandMock);
        when(requestHeadersSpecForestMock.exchange()).thenReturn(Mono.just(getClientResponse("forest")));
        when(requestHeadersSpecCarMock.exchange()).thenReturn(Mono.just(getClientResponse("car")));
        when(requestHeadersSpecLandMock.exchange()).thenReturn(Mono.just(getClientResponse("land")));
        when(requestHeadersUriSpecMock.uri(contains("?i=")))
                .thenReturn(requestHeadersSpecMovieMock);

        when(requestHeadersSpecMovieMock.exchange()).thenReturn(Mono.just(movieClientResponse));

        when(movieRepository.save(any(Movie.class))).thenReturn(Mono.just(movie));
        when(movieKeywordRepository.save(any(MovieKeyword.class))).thenReturn(Mono.just(new MovieKeyword()));

        StepVerifier.create(movieFactoryService.generateMovieFeeds(3))
                .expectSubscription()
                .expectNextCount(3L)
                .verifyComplete();

        verify(movieKeywordRepository, times(3)).save(any(MovieKeyword.class));


    }

}