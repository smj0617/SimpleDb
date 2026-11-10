package com.ll;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;
    private boolean isBlind;
}
