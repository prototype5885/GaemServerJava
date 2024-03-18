package org.ProToType;

import org.ProToType.Classes.ConfigFileContent;

import java.io.*;

public class ConfigFile {
    public static ConfigFileContent readConfigFile() {
        ConfigFileContent configFileContent = new ConfigFileContent();
        String configFilename = "config.txt";
        File configFile = new File(configFilename);

        // runs if config file doesn't exist
        if (!configFile.exists()) {
            System.out.println("Config file doesn't exist, creating new...");

            try {
                configFile.createNewFile();
                FileWriter writer = new FileWriter(configFilename);
                writer.write("tcpPort=1942\n");
                writer.write("tickRate=10\n");
                writer.write("maxPlayers=10\n");
                writer.write("encryptionKey=0123456789ABCDEF0123456789ABCDEF\n");
                writer.close();
                System.out.println("Config file created");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // runs if or when config file exists
        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    switch (key) {
                        case "tcpPort":
                            configFileContent.tcpPort = Integer.parseInt(parts[1].trim());
                            break;
                        case "tickRate":
                            configFileContent.tickRate = Integer.parseInt(parts[1].trim());
                            break;
                        case "maxPlayers":
                            configFileContent.maxPlayers = Integer.parseInt(parts[1].trim());
                            break;
                        case "encryptionKey":
                            configFileContent.encryptionKey = parts[1].trim();
                            break;
                    }

                }
            }
            reader.close();
            System.out.println("Config file read");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return configFileContent;
    }
}