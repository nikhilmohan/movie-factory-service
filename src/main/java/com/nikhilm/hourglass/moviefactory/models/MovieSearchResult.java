package com.nikhilm.hourglass.moviefactory.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MovieSearchResult {
    @JsonProperty("Search")
    private List<MovieSummary> search;
}
