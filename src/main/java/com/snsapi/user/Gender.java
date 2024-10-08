package com.snsapi.user;

import lombok.Getter;

@Getter
public enum Gender {
    MALE(0),
    FEMALE(1);

    private final int value;

    Gender(int value) {
        this.value = value;
    }

}