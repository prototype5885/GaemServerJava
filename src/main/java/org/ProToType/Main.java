package org.ProToType;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static final ObjectMapper jackson = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        new Server();
    }
}