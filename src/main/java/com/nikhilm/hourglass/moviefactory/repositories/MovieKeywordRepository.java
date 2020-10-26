package com.nikhilm.hourglass.moviefactory.repositories;

import com.nikhilm.hourglass.moviefactory.models.Movie;
import com.nikhilm.hourglass.moviefactory.models.MovieKeyword;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface MovieKeywordRepository extends ReactiveMongoRepository<MovieKeyword, String> {
}
