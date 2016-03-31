package com.zabo.post;

import java.util.concurrent.CountDownLatch;

/**
 * Created by zhaoboliu on 3/22/16.
 */
public enum City {
    San_Francisco("San Francisco", "SF", State.California, Country.United_State),
    San_Jose("San Jose", "SJ", State.California, Country.United_State),
    Los_Angeles("Los Angeles", "LA", State.California, Country.United_State);

    private String name;
    private String short_name;
    private State state;
    private Country country;

    City(String name, String short_name, State state, Country country) {
        this.name = name;
        this.short_name = short_name;
        this.state = state;
        this.country = country;
    }
    @Override
    public String toString() {
        return name;
    }
    public enum Country {
        United_State("United State", "US"),
        China("China", "CN");

        private String name;
        private String short_name;
        Country(String name, String short_name){
            this.name = name;
            this.short_name = short_name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum State {
        California("California", "CA", Country.United_State),
        New_York("New York", "NY", Country.United_State),
        Washionton("Washionton", "WA", Country.United_State);

        private String name;
        private String short_name;
        private Country country;
        State(String name, String short_name, Country country){
            this.name = name;
            this.short_name = short_name;
            this.country = country;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
