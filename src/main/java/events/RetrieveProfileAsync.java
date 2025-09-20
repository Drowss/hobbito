package events;

import interfaces.ICommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.awt.*;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
public class RetrieveProfileAsync implements ICommand {

    private final WebClient webClient;

    public RetrieveProfileAsync() {
        this.webClient = WebClient
                .builder()
                .baseUrl("https://www.hobba.tv")
                .build();
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public String getDescription() {
        return "Muestra el perfil del Hobba usuario";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(
                OptionType.STRING,
                "Usuario",
                "Usuario a buscar",
                true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue(interactionHook -> {
            String usuario = event.getOption("usuario").getAsString();
            String avatarUrl = "https://www.hobba.tv/habblet/avatarimageByUsername/" + usuario +
                    "?size=undefined&direction=0&head_direction=2&headonly=0&action=std&gesture=std";

            webClient.get()
                    .uri("/habblet/avatarimageByUsername/{usuario}?size=undefined&direction=0&head_direction=2&headonly=0&action=std&gesture=std",
                            usuario)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, error -> {
                        EmbedBuilder embed = this.buildFrank("Ocurrió un error inesperado, intenta de nuevo más tarde.");
                        interactionHook.sendMessageEmbeds(embed.build()).queue();
                        return Mono.error(new RuntimeException("Error al obtener la imagen del usuario"));
                    })
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(response -> {
                        HttpHeaders headers = response.getHeaders();
                        if (!Objects.equals(headers.getContentType(), MediaType.IMAGE_PNG)){
                            EmbedBuilder embed = this.buildFrank("No encontramos al usuario en ninguna habitación del hotel,\n ¿estás seguro que existe?");
                            interactionHook.sendMessageEmbeds(embed.build()).queue();
                        } else if (Objects.equals(headers.getContentType(), MediaType.IMAGE_PNG)) {
                            EmbedBuilder embed = this.buildSuccesProfile(avatarUrl, usuario, event);

                            interactionHook.sendMessageEmbeds(embed.build()).queue();
                        }
                    })
                    .doOnError(throwable -> {
                        log.error("Error inesperado " + throwable.getMessage());
                    })
                    .subscribe();
        });
    }

    private EmbedBuilder buildFrank(String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setImage("https://i.imgur.com/lbLR6Pu.png")
                .setTitle("\uD83D\uDE31 Algo ha preocupado a Frank!")
                .setDescription(description)
                .setColor(Color.RED);
        return embed;
    }

    private EmbedBuilder buildSuccesProfile(String avatarUrl, String usuario, SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setImage(avatarUrl)
                .setTitle("👤 Te presentamos a " + usuario + " ! :)")
                .setDescription(event.getMember().getEffectiveName() + " moría por ver como luce hoy " + usuario)
                .setColor(Color.GREEN)
                .setFooter("https://hobba.tv",
                        "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png?ex=68c4cc9f&is=68c37b1f&hm=e11645338c8b0e57ea47132114bf3917206b855f0fbb347a125a0e3d89657a37&=&format=webp&quality=lossless");

        embed.addField("🎭 Amigos: 69",
                "♥️ **myoui** (y otros 1)\n" +
                        "☺️ **xDraken**\n" +
                        "💀 **Enyo** (y otros 12)\n" +
                        "💩 **grieta**", false);

        embed.addField("📊 Perfil del usuario",
                "**Creado:** 25/11/2017\n" +
                        "**Último inicio de sesión:** Hace 11 horas\n" +
                        "**Recompensas:** 28165\n" +
                        "**Respetos:** 1250\n" +
                        "**Nivel de pase:** 13", true);

        return embed;
    }
}
