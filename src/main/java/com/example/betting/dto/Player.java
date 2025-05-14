package com.example.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class Player {

    private String name;
    private String id;
    private double winnings;
    private double originalBalance;
    private double balance;
    private double rtp;
}
