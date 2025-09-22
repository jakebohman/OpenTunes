package com.spotify.clone.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration manager for the application
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "spotify-clone.properties";
    private static ConfigManager instance;
    private Properties properties;
    
    private ConfigManager() {
        properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        }
        
        // Set default values
        setDefaultValues();
    }
    
    private void setDefaultValues() {
        if (!properties.containsKey("volume")) {
            properties.setProperty("volume", "0.5");
        }
        if (!properties.containsKey("lastImportPath")) {
            properties.setProperty("lastImportPath", System.getProperty("user.home"));
        }
        if (!properties.containsKey("windowWidth")) {
            properties.setProperty("windowWidth", "1200");
        }
        if (!properties.containsKey("windowHeight")) {
            properties.setProperty("windowHeight", "800");
        }
    }
    
    public void saveConfig() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Spotify Clone Configuration");
        }
    }

    /**
     * Safe wrapper to persist config without throwing (returns success boolean).
     */
    public boolean saveConfigSafe() {
        try {
            saveConfig();
            return true;
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
            return false;
        }
    }
    
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public void setString(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }
    
    public void setDouble(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
    }
}