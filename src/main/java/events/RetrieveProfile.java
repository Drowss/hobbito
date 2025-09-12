package events;

import interfaces.ICommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;

@Slf4j
public class RetrieveProfile implements ICommand {
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
        try {
            String user = Objects.requireNonNull(event.getOption("usuario")).getAsString();
            String avatarUrl = "https://www.hobba.tv/habblet/avatarimageByUsername/" + user + "?size=undefined&direction=0&head_direction=2&headonly=0&action=std&gesture=std";
            log.info("Ejecutando comando 'profile' por " + event.getUser().getEffectiveName() + " buscando a " + user);
            this.isValidImage(avatarUrl);

            EmbedBuilder embed = new EmbedBuilder()
                    .setImage(avatarUrl)
                    .setTitle("👤 Te presentamos a " + user + " ! :)")
                    .setDescription(event.getMember().getEffectiveName() + " moría por ver como luce hoy " + user)
                    .setColor(Color.PINK)
                    .setFooter("https://hobba.tv",
                            "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png?ex=68c4cc9f&is=68c37b1f&hm=e11645338c8b0e57ea47132114bf3917206b855f0fbb347a125a0e3d89657a37&=&format=webp&quality=lossless");

            // SECCIÓN DE AMIGOS (arriba)
            embed.addField("🎭 Amigos: 69",
                    "♥️ **myoui** (y otros 1)\n" +
                            "☺️ **xDraken**\n" +
                            "💀 **Enyo** (y otros 12)\n" +
                            "💩 **grieta**", false);

            // ✅ TEXTO DEBAJO DE LA IMAGEN
            embed.addField("📊 Perfil del usuario",
                    "**Creado:** 25/11/2017\n" +
                            "**Último inicio de sesión:** Hace 11 horas\n" +
                            "**Recompensas:** 28165\n" +
                            "**Respetos:** 1250\n" +
                            "**Nivel de pase:** 13", true);

            event.replyEmbeds(embed.build()).queue();
        } catch (Exception e) {
            log.error("Error inesperado " + e.getMessage());
            EmbedBuilder embed = new EmbedBuilder()
                    .setImage("https://i.imgur.com/lbLR6Pu.png")
                    .setTitle("\uD83D\uDE31 Algo ha preocupado a Frank!")
                    .setDescription(e.getMessage())
                    .setColor(Color.RED);
            event.replyEmbeds(embed.build()).queue();
        }

    }

    private void isValidImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        String contentType = connection.getContentType();
        if (!(contentType != null && contentType.startsWith("image/"))) {
            log.warn("La URL no es una imagen válida: " + imageUrl);
            throw new RuntimeException("No encontramos al usuario en ninguna habitación del hotel,\n ¿estás seguro que existe?");
        }
    }
}
