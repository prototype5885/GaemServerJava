package org.ProToType.Instanceables;

import org.ProToType.Static.Shortcuts;

import java.io.*;

public class ConfigFile {

    public int tcpPort = 1942;
    public int tickRate = 10;
    public int maxPlayers = 128;
    public String encryptionKey = "0123456789ABCDEF0123456789ABCDEF";
    public String databaseType = "h2";
    public String remoteDatabaseIpAddress = "127.0.0.1";
    public String remoteDatabasePort = "3306";
    public String dbUsername = "username";
    public String dbPassword = "password";

    public ConfigFile() throws IOException {
        final String configFilename = "config.txt";
        final File configFile = new File(configFilename);

        // runs if config file doesn't exist
        final String tcpPortString = "tcpPort";
        final String tickRateString = "tickRate";
        final String maxPlayersString = "maxPlayers";
        final String encryptionKeyString = "encryptionKey";
        final String databaseTypeString = "databaseType";
        final String remoteDatabaseIpAddressString = "remoteDatabaseIpAddress";
        final String remoteDatabasePortString = "remoteDatabasePort";
        final String dbUsernameString = "dbUsername";
        final String dbPasswordString = "dbPassword";

        if (!configFile.exists()) {
            Shortcuts.PrintWithTime("Config file doesn't exist, creating new...");

            configFile.createNewFile();
            final FileWriter writer = new FileWriter(configFilename);

            writer.write(FormatConfig(tcpPortString, String.valueOf(tcpPort)));
            writer.write(FormatConfig(tickRateString, String.valueOf(tickRate)));
            writer.write(FormatConfig(maxPlayersString, String.valueOf(maxPlayers)));
            writer.write(FormatConfig(encryptionKeyString, encryptionKey));
            writer.write(FormatConfig(databaseTypeString, databaseType));
            writer.write(FormatConfig(remoteDatabaseIpAddressString, remoteDatabaseIpAddress));
            writer.write(FormatConfig(remoteDatabasePortString, remoteDatabasePort));
            writer.write(FormatConfig(dbUsernameString, dbUsername));
            writer.write(FormatConfig(dbPasswordString, dbPassword));

            writer.close();
            Shortcuts.PrintWithTime("Config file created, reading config file now...");

        }

        // runs if or when config file exists
        String line;


        final BufferedReader reader = new BufferedReader(new FileReader(configFile));
        while ((line = reader.readLine()) != null) {
            final String[] parts = line.split("=");
            if (parts.length == 2) {
                final String key = parts[0].trim();
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
                    case databaseTypeString:
                        databaseType = parts[1].trim();
                        break;
                    case remoteDatabaseIpAddressString:
                        remoteDatabaseIpAddress = parts[1].trim();
                        break;
                    case remoteDatabasePortString:
                        remoteDatabasePort = parts[1].trim();
                        break;
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
        Shortcuts.PrintWithTime("Config file read successfully");
    }

    private String FormatConfig(String name, String value) {
        return name + "=" + value + "\n";
    }
}