package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@Configuration
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            LOGGER.info("No DATABASE_URL found. Falling back to default database properties (H2).");
            return DataSourceBuilder.create()
                    .url("jdbc:h2:file:./data/librarydb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE")
                    .username("sa")
                    .password("")
                    .driverClassName("org.h2.Driver")
                    .build();
        }

        try {
            LOGGER.info("DATABASE_URL environment variable found. Parsing PostgreSQL credentials...");
            
            // Clean Replit connection URI if it has wrong prefixes
            if (databaseUrl.startsWith("postgres://")) {
                databaseUrl = databaseUrl.replace("postgres://", "postgresql://");
            }
            
            URI dbUri = new URI(databaseUrl);
            String userInfo = dbUri.getUserInfo();
            String username = "";
            String password = "";
            
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":");
                username = parts[0];
                password = parts[1];
            } else if (userInfo != null) {
                username = userInfo;
            }

            // Construct jdbc URL: jdbc:postgresql://host:port/database
            String host = dbUri.getHost();
            int port = dbUri.getPort();
            if (port == -1) {
                port = 5432; // Default Postgres port
            }
            String path = dbUri.getPath();
            
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
            
            LOGGER.info("PostgreSQL JDBC URL configured successfully: " + jdbcUrl);

            return DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("org.postgresql.Driver")
                    .build();
        } catch (URISyntaxException | ArrayIndexOutOfBoundsException e) {
            LOGGER.severe("Failed to parse DATABASE_URL: " + e.getMessage() + ". Falling back to H2 database.");
            return DataSourceBuilder.create()
                    .url("jdbc:h2:file:./data/librarydb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE")
                    .username("sa")
                    .password("")
                    .driverClassName("org.h2.Driver")
                    .build();
        }
    }
}
