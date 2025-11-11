import adapter.GetUserAdapter;
import cmdmanager.SlashCommands;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import common.HobbaUserCode;
import events.AccountVerification;
import interfaces.ICommand;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static common.Components.CAFFEINE_TTL;


@Slf4j
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Dotenv dotenv = null;
        String token;

        try {
            dotenv = Dotenv.load();
            token = dotenv.get("DISCORD_BOT_TOKEN");
        } catch (Exception e) {
            token = null;
        }

        if (token == null || token.isEmpty()) {
            token = System.getenv("DISCORD_BOT_TOKEN");
        }

        if (token == null || token.isEmpty()) {
            log.error("No se encontró DISCORD_BOT_TOKEN en variables de entorno");
            System.exit(1);
        }

        Cache<String, HobbaUserCode> cache = Caffeine.newBuilder()
                .expireAfterWrite(CAFFEINE_TTL, TimeUnit.MINUTES)
                .build();

        GetUserAdapter getUserAdapter = new GetUserAdapter(dotenv);

        List<ICommand> commandList = List.of(
                new AccountVerification(cache, getUserAdapter)
        );

//        for (ICommand command : commandList) {
//            if (command instanceof Help) {
//                ((Help) command).setCommands(commandList);
//            }
//        }

        JDA jda = JDABuilder.createDefault(token,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("Snowstorm"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(new SlashCommands(commandList))
                .setAutoReconnect(true)
                .build();
        jda.awaitReady();

        List<ICommand> globalCommands = commandList.stream()
                .filter(cmd -> !cmd.getName().equalsIgnoreCase("verificar"))
                .toList();

        jda.updateCommands()
                .addCommands(
                        globalCommands.stream()
                                .map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription())
                                        .addOptions(cmd.getOptions()))
                                .toList()
                )
                .queue();

        List<String> allowedGuilds = List.of(
                "1415543422588031119",
                "640539915674714123"
        );

        allowedGuilds.forEach(guildId -> {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                guild.updateCommands()
                        .addCommands(
                                commandList.stream()
                                        .map(cmd -> Commands.slash(cmd.getName(), cmd.getDescription())
                                                .addOptions(cmd.getOptions()))
                                        .toList()
                        )
                        .queue(x -> log.info("🏠 Comandos registrados en servidor: " + guild.getName()));
            } else {
                log.error("⚠️ El bot no está en el servidor con ID {}", guildId);
            }
        });
    }
}
