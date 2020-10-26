package com.nikhilm.hourglass.moviefactory.repositories;

import com.nikhilm.hourglass.moviefactory.models.Movie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface MovieRepository extends ReactiveMongoRepository<Movie, String> {
}
