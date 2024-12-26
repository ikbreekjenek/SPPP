package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

@SpringBootApplication
public class SpringConsoleApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringConsoleApp.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(CommandHandler commandHandler) {
        return args -> {
            commandHandler.start();
        };
    }
}

@Configuration
@PropertySource("classpath:application.properties")
class AppConfig {
}

@Service
class CommandHandler {
    private final MessageSource messageSource;
    private final EntityService entityService;


    private Locale locale = Locale.getDefault();

    @Autowired
    public CommandHandler(MessageSource messageSource, EntityService entityService) {
        this.messageSource = messageSource;
        this.entityService = entityService;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println(messageSource.getMessage("prompt.command", null, locale));
            String command = scanner.nextLine();

            if (command.equalsIgnoreCase("find-all")) {
                entityService.findAll().forEach(entity -> System.out.println(formatEntity(entity)));
            } else if (command.startsWith("find")) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[1]);
                        Entity entity = entityService.findById(id);
                        if (entity != null) {
                            System.out.println(formatEntity(entity));
                        } else {
                            System.out.println(messageSource.getMessage("error.not.found", null, locale));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(messageSource.getMessage("error.invalid.id", null, locale));
                    }
                } else {
                    System.out.println(messageSource.getMessage("error.missing.parameter", null, locale));
                }
            } else if (command.startsWith("lang")) {
                String[] parts = command.split(" ", 2);
                if (parts.length == 2) {
                    switchLocale(parts[1]);
                } else {
                    System.out.println(messageSource.getMessage("error.missing.parameter", null, locale));
                }
            } else if (command.equalsIgnoreCase("exit")) {
                break;
            } else {
                System.out.println(messageSource.getMessage("error.unknown.command", null, locale));
            }
        }
    }

    private void switchLocale(String lang) {
        switch (lang.toLowerCase()) {
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "ru":
                locale = new Locale("ru", "RU");
                break;
            default:
                System.out.println(messageSource.getMessage("error.unknown.language", null, locale));
                return;
        }
        System.out.println(messageSource.getMessage("info.language.changed", new Object[]{locale.getDisplayLanguage(locale)}, locale));
    }

    private String formatEntity(Entity entity) {
        return messageSource.getMessage("entity.format", new Object[]{entity.getId(), entity.getName()}, locale);
    }
}

@Service
class EntityService {
    private final EntityDao entityDao;
    public int addEntity(String name) {
        return entityDao.addEntity(name);
    }


    @Autowired
    public EntityService(EntityDao entityDao) {
        this.entityDao = entityDao;
    }

    public List<Entity> findAll() {
        return entityDao.findAll();
    }

    public Entity findById(int id) {
        return entityDao.findById(id);
    }
}

@Repository
class EntityDao {
    private final JdbcTemplate jdbcTemplate;
    public int addEntity(String name) {
        return jdbcTemplate.update("INSERT INTO entities (name) VALUES (?)", name);
    }


    @Autowired
    public EntityDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Entity> findAll() {
        return jdbcTemplate.query("SELECT * FROM entities", (rs, rowNum) -> new Entity(rs.getInt("id"), rs.getString("name")));
    }

    public Entity findById(int id) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM entities WHERE id = ?", new Object[]{id}, (rs, rowNum) -> new Entity(rs.getInt("id"), rs.getString("name")));
        } catch (DataAccessException e) {
            return null;
        }
    }
}

class Entity {
    private final int id;
    private final String name;

    public Entity(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
