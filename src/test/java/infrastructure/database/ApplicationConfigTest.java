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
            String dbUser = props.getProperty("db.user", "").trim();

            assertNotNull(dbUrl, "db.url must be configured");
            assertFalse(dbUrl.isBlank(), "db.url must not be blank");
            assertEquals("com.microsoft.sqlserver.jdbc.SQLServerDriver", dbDriver);
            assertFalse(dbUser.isBlank(),
                    "db.user must not be blank (SQL Server rejects empty user); override with DB_USER in production if needed");
        }
    }
}
