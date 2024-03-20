package org.ProToType;

import org.ProToType.Static.PrintWithTime;

import java.io.*;

public class ConfigFile {
    private final String tcpPortString = "tcpPort";
    private final String tickRateString = "tickRate";
    private final String maxPlayersString = "maxPlayers";
    private final String encryptionKeyString = "encryptionKey";
    private final String localDatabaseString = "localDatabase";
    private final String remoteDatabaseIpAddressString = "remoteDatabaseIpAddress";
    private final String remoteDatabasePortString = "remoteDatabasePort";
    private final String dbUsernameString = "dbUsername";
    private final String dbPasswordString = "dbPassword";

    public int tcpPort;
    public int tickRate;
    public int maxPlayers;
    public String encryptionKey;
    public boolean localDatabase;
    public String remoteDatabaseIpAddress;
    public String remoteDatabasePort;
    public String dbUsername;
    public String dbPassword;


    public ConfigFile() throws IOException {
        String configFilename = "config.txt";
        File configFile = new File(configFilename);

        // runs if config file doesn't exist
        if (!configFile.exists()) {
            PrintWithTime.Print("Config file doesn't exist, creating new...");

            configFile.createNewFile();
            FileWriter writer = new FileWriter(configFilename);

            writer.write(FormatConfig(tcpPortString, String.valueOf(1942)));
            writer.write(FormatConfig(tickRateString, String.valueOf(10)));
            writer.write(FormatConfig(maxPlayersString, String.valueOf(10)));
            writer.write(FormatConfig(encryptionKeyString, "0123456789ABCDEF0123456789ABCDEF"));
            writer.write(FormatConfig(localDatabaseString, String.valueOf(true)));
            writer.write(FormatConfig(remoteDatabaseIpAddressString, "127.0.0.1"));
            writer.write(FormatConfig(remoteDatabasePortString, String.valueOf(3306)));
            writer.write(FormatConfig(dbUsernameString, "username"));
            writer.write(FormatConfig(dbPasswordString, "password"));

            writer.close();
            PrintWithTime.Print("Config file created");

        }

        // runs if or when config file exists
        String line;

        BufferedReader reader = new BufferedReader(new FileReader(configFile));
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("=");
            if (parts.length == 2) {
                String key = parts[0].trim();
                switch (key) {
                    case tcpPortString:
                        tcpPort = Integer.parseInt(parts[1].trim());
                        break;
                    case tickRateString:
                        tickRate = Integer.parseInt(parts[1].trim());
                        break;
                    case maxPlayersString:
                        maxPlayers = Integer.parseInt(parts[1].trim());
                        break;
                    case encryptionKeyString:
                        encryptionKey = parts[1].trim();
                        break;
                    case localDatabaseString:
                        localDatabase = Boolean.parseBoolean(parts[1].trim());
                        break;
                    case remoteDatabaseIpAddressString:
                        remoteDatabaseIpAddress = parts[1].trim();
                        break;
                    case remoteDatabasePortString:
                        remoteDatabasePort = parts[1].trim();
                    case dbUsernameString:
                        dbUsername = parts[1].trim();
                        break;
                    case dbPasswordString:
                        dbPassword = parts[1].trim();
                        break;
                }
            }
        }
        reader.close();
        System.out.println("Config file read");
    }

    private String FormatConfig(String name, String value) {
        return name + "=" + value + "\n";
    }
}