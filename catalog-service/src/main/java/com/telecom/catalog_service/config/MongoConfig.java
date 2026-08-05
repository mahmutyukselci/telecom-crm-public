package com.telecom.catalog_service.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

    private final Environment env;

    public MongoConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public MongoClient mongoClient() {
        // Checks properties/yml or system environment variables based on priority
        String uri = env.getProperty("spring.data.mongodb.uri");

        // If the value is null or incorrectly defined as plain text, fix it
        if (uri == null || (!uri.startsWith("mongodb://") && !uri.startsWith("mongodb+srv://"))) {
            System.out.println(">>> [MongoConfig] YML/Env not found or invalid. Falling back to local IDE address (localhost).");

            // If environment variables exist, use them for Docker; otherwise fallback to localhost
            String user = env.getProperty("MONGO_ROOT_USER", "admin");
            String pass = env.getProperty("MONGO_ROOT_PASSWORD", "admin_password");
            String host = env.getProperty("MONGO_HOST", "localhost");

            uri = String.format(
                    "mongodb://%s:%s@%s:27017/catalog_db?authSource=admin",
                    user, pass, host
            );
        }

        System.out.println(">>> ACTUAL MONGODB URI USED BY THE APPLICATION: " + uri);
        return MongoClients.create(uri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        String databaseName = env.getProperty("spring.data.mongodb.database", "catalog_db");
        return new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(mongoClient(), databaseName)
        );
    }
}