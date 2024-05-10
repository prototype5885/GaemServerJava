package org.ProToType.Instanceables;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigFile {

    private static final Logger logger = LogManager.getLogger(ConfigFile.class);

    public int tcpPort;
    public int tickRate;
    public int maxPlayers;
    //    public String encryptionKey;
    public String databaseType;
    public String remoteDatabaseIpAddress;
    public String remoteDatabasePort;
    public String dbUsername;
    public String dbPassword;

    public ConfigFile() throws Exception {
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

        // creates config file if it doesnt exist
        logger.debug("Looking for config file...");
        if (configFile.exists()) {
            logger.debug("Config file found");
        } else {
            logger.debug("Config file doesn't exist, creating new...");

            configFile.createNewFile();
            final FileWriter writer = new FileWriter(configFilename);

            writer.write(FormatConfig(tcpPortString, String.valueOf(1942)));
            writer.write(FormatConfig(tickRateString, String.valueOf(10)));
            writer.write(FormatConfig(maxPlayersString, String.valueOf(128)));
            writer.write(FormatConfig(encryptionKeyString, "0123456789ABCDEF0123456789ABCDEF"));
            writer.write(FormatConfig(databaseTypeString, "sqlite"));
            writer.write(FormatConfig(remoteDatabaseIpAddressString, "127.0.0.1"));
            writer.write(FormatConfig(remoteDatabasePortString, "3306"));
            writer.write(FormatConfig(dbUsernameString, "username"));
            writer.write(FormatConfig(dbPasswordString, "password"));

            writer.close();
            logger.debug("New config file was created");
        }

        // runs if or when config file exists
        logger.debug("Reading config file...");
        String line;

        final BufferedReader reader = new BufferedReader(new FileReader(configFile));
        while ((line = reader.readLine()) != null) {
            final String[] parts = line.split("=");
            if (parts.length == 2) {
                final String key = parts[0].trim();
                final String value = parts[1].trim();
                switch (key) {
                    case tcpPortString:
                        tcpPort = Integer.parseInt(value);
                        break;
                    case tickRateString:
                        tickRate = Integer.parseInt(value);
                        break;
                    case maxPlayersString:
                        maxPlayers = Integer.parseInt(value);
                        break;
//                    case encryptionKeyString:
//                        encryptionKey = parts[1].trim();
//                        break;
                    case databaseTypeString:
                        databaseType = value;
                        break;
                    case remoteDatabaseIpAddressString:
                        remoteDatabaseIpAddress = value;
                        break;
                    case remoteDatabasePortString:
                        remoteDatabasePort = value;
                        break;
                    case dbUsernameString:
                        dbUsername = value;
                        break;
                    case dbPasswordString:
                        dbPassword = value;
                        break;
                }
            }
        }

        // checks if each values were read successfully
        List<String> missingValues = new ArrayList<>();

        if (tcpPort != 0) {
            logger.debug("{}: {}", tcpPortString, tcpPort);
        } else {
            missingValues.add(tcpPortString);
        }

        if (tickRate != 0) {
            logger.debug("{}: {}", tickRateString, tickRate);
        } else {
            missingValues.add(tickRateString);
        }

        if (maxPlayers != 0) {
            logger.debug("{}: {}", maxPlayersString, maxPlayers);
        } else {
            missingValues.add(maxPlayersString);
        }

//        if (encryptionKey != null) {
//            logger.debug("{}: {}", encryptionKeyString, encryptionKey);
//        } else {
//            missingValues.add(encryptionKeyString);
//        }

        if (databaseType != null) {
            logger.debug("{}: {}", databaseTypeString, databaseType);
        } else {
            missingValues.add(databaseTypeString);
        }

        // these only get checked if type isn't sqlite as it doesn't require
        // authentication
        if (databaseType != null && !databaseType.equals("sqlite")) {
            if (remoteDatabaseIpAddress != null) {
                logger.debug("{}: {}", remoteDatabaseIpAddressString, remoteDatabaseIpAddress);
            } else {
                missingValues.add(remoteDatabaseIpAddressString);
            }

            if (remoteDatabasePort != null) {
                logger.debug("{}: {}", remoteDatabasePortString, remoteDatabasePort);
            } else {
                missingValues.add(remoteDatabasePortString);
            }

            if (dbUsername != null) {
                logger.debug("{}: {}", dbUsernameString, dbUsername);
            } else {
                missingValues.add(dbUsernameString);
            }

            if (dbPassword != null) {
                logger.debug("{}: {}", dbPasswordString, dbPassword);
            } else {
                missingValues.add(dbPasswordString);
            }
        } else {
            logger.debug("{} is {}, extra authentication is not needed", databaseTypeString, databaseType);
        }
        reader.close();

        if (!missingValues.isEmpty()) {
            for (String missingConfigValue : missingValues) {
                logger.fatal("{} is missing from config file", missingConfigValue);
            }
            throw new Exception("Incomplete config file");
        } else {
            logger.info("Config file read successfully");
        }

    }

    private String FormatConfig(String name, String value) {
        return name + "=" + value + "\n";
    }
}