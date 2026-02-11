package com.drow.hobbito.config;

import com.drow.hobbito.adapter.GetUserAdapter;
import com.drow.hobbito.cmdmanager.SlashCommands;
import com.drow.hobbito.common.HobbaUserCode;
import com.drow.hobbito.events.AccountVerification;
import com.drow.hobbito.events.Giveaway;
import com.drow.hobbito.events.Tickets;
import com.drow.hobbito.events.VouchersByPeriod;
import com.drow.hobbito.interfaces.ICommand;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.drow.hobbito.common.Components.CAFFEINE_TTL;

@Configuration
@Slf4j
public class BotConfig {
    @Bean
    public GetUserAdapter getUserAdapter(@Qualifier("getUserAdapterWebClient") WebClient webClient, Dotenv dotenv) {
        return new GetUserAdapter(webClient, dotenv);
    }

    @Bean(name = "getUserAdapterWebClient")
    public WebClient getUserAdapterWebClient() {
        return WebClient
                .builder()
                .baseUrl("https://api.hobba.tv")
                .build();
    }

    @Bean(name = "allDiscordCommands")
    public List<ICommand> allDiscordCommands(Cache<String, HobbaUserCode> cache, GetUserAdapter getUserAdapter) {
        return List.of(
                new AccountVerification(cache, getUserAdapter),
                new Giveaway(),
                new VouchersByPeriod()
        );
    }

    @Bean(name = "publicDiscordCommands")
    public List<ICommand> publicDiscordCommands(@Qualifier("allDiscordCommands") List<ICommand> allDiscordCommands) {
        return allDiscordCommands.stream()
                .filter(cmd -> !cmd.getName().equalsIgnoreCase("verificar")
                        && !cmd.getName().equalsIgnoreCase("sorteo"))
                .toList();
    }

    @Bean
    public List<String> allowedGuildsToUseAllDiscordCommands() {
        return List.of(
                "1415543422588031119",
                "640539915674714123"
        );
    }

    @Bean
    public Cache<String, HobbaUserCode> caffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(CAFFEINE_TTL, TimeUnit.MINUTES)
                .build();
    }

    @Bean
    public Dotenv dotenv() {
        Dotenv dotenv;
        try {
            dotenv = Dotenv.load();
        } catch (Exception e) {
            dotenv = null;
        }
        return dotenv;
    }

    @Bean
    public JDA jda(@Qualifier("allDiscordCommands") List<ICommand> allDiscordCommands, Dotenv dotenv, List<String> allowedGuildsToUseAllDiscordCommands) {
        String token = dotenv.get("DISCORD_BOT_TOKEN");

        if (token == null || token.isEmpty()) {
            log.error("No se encontró DISCORD_BOT_TOKEN en variables de entorno");
            return null;
        }

        return JDABuilder.createDefault(token,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.playing("Snowstorm"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(new SlashCommands(allDiscordCommands))
                .addEventListeners(new Tickets(allowedGuildsToUseAllDiscordCommands))
                .addEventListeners(new Giveaway())
                .setAutoReconnect(true)
                .build();
    }
}
