import org.flywaydb.core.Flyway;

public class RepairMigrationHistory {
  public static void main(String[] arguments) {
    String databaseUrl = arguments[0];
    String databaseUsername = arguments[1];
    String migrationDirectory = arguments[2];
    String databasePassword = System.getenv("DB_PASSWORD");
    if (databasePassword == null || databasePassword.isBlank()) {
      throw new IllegalArgumentException("Defina DB_PASSWORD para o banco local");
    }
    var flyway = Flyway.configure().dataSource(databaseUrl, databaseUsername, databasePassword)
        .locations("filesystem:" + migrationDirectory).load();
    flyway.repair();
    flyway.validate();
    System.out.println("MIGRATION_HISTORY=REPAIRED_AND_VALIDATED");
  }
}