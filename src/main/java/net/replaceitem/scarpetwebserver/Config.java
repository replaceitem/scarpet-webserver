package net.replaceitem.scarpetwebserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Config {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static Config load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("scarpet-webserver.json");
        try(FileReader reader = new FileReader(configPath.toFile())) {
            return GSON.fromJson(reader, Config.class);
        } catch (FileNotFoundException fileNotFoundException) {
            Config config = new Config();
            try(FileWriter writer = new FileWriter(configPath.toFile())) {
                config.save(writer);
            } catch (IOException e) {
                ScarpetWebserver.LOGGER.error("Could not create config", e);
            }
            return config;
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            ScarpetWebserver.LOGGER.error("Could not load config", e);
            return new Config();
        }
    }

    private void save(Appendable appendable) throws JsonIOException {
        GSON.toJson(this, appendable);
    }
    
    public final List<WebserverConfig> webservers = List.of(new WebserverConfig("myserver",-1));

    public static class WebserverConfig {

        public WebserverConfig(String id, int port) {
            this.id = id;
            this.port = port;
        }

        public String id;
        public int port;
    }
}
