package infrastructure.database;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationConfigTest {

    @Test
    void applicationPropertiesShouldExistAndUseSafePasswordDefault() throws Exception {
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            assertNotNull(is, "application.properties must be available on classpath");

            Properties props = new Properties();
            props.load(is);

            String dbUrl = props.getProperty("db.url");
            String dbDriver = props.getProperty("db.driver");
            String dbPassword = props.getProperty("db.password", "");

            assertNotNull(dbUrl, "db.url must be configured");
            assertFalse(dbUrl.isBlank(), "db.url must not be blank");
            assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", dbDriver);
            assertTrue(dbPassword.isBlank(), "db.password should be blank in repository; use env variable");
        }
    }
}
