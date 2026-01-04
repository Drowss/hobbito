package com.drow.hobbito;

import com.drow.hobbito.interfaces.ICommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@Slf4j
public class Main implements CommandLineRunner {
    private final JDA jda;
    private final List<String> allowedGuilds;
    @Qualifier("publicDiscordCommands")
    private final List<ICommand> globalCommands;
    @Qualifier("allDiscordCommands")
    private final List<ICommand> allDiscordCommands;

    public Main(
            JDA jda,
            List<String> allowedGuilds,
            @Qualifier("publicDiscordCommands") List<ICommand> globalCommands,
            @Qualifier("allDiscordCommands") List<ICommand> allDiscordCommands
    ) {
        this.jda = jda;
        this.allowedGuilds = allowedGuilds;
        this.globalCommands = globalCommands;
        this.allDiscordCommands = allDiscordCommands;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        jda.awaitReady();
        jda.updateCommands()
                .addCommands(
                        globalCommands.stream()
                                .map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription())
                                        .addOptions(cmd.getOptions()))
                                .toList()
                )
                .queue();

        allowedGuilds.forEach(guildId -> {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands()
                        .addCommands(
                                allDiscordCommands.stream()
                                        .map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription())
                                                .addOptions(cmd.getOptions()))
                                        .toList()
                        )
                        .queue(x -> log.info("🏠 Comandos registrados en servidor: {}", guild.getName()));
            }
        });
    }
}
