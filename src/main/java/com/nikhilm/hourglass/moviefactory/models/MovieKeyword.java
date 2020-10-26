package com.nikhilm.hourglass.moviefactory.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "moviekeywords")
public class MovieKeyword {
    @Id
    private String id;
    private String keyword;
    private int lastPageAccessed = 0;
}
