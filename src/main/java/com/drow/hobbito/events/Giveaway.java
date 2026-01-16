package com.drow.hobbito.events;

import com.drow.hobbito.interfaces.ICommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.drow.hobbito.common.Components.COLORS;
import static com.drow.hobbito.common.Components.FOOTER_ICON;

@Slf4j
@RequiredArgsConstructor
public class Giveaway extends ListenerAdapter implements ICommand {
    private static final long GIVEAWAY_ROLE_ID = 1461597804660129877L;
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    @Override
    public String getName() {
        return "sorteo";
    }

    @Override
    public String getDescription() {
        return "Realiza un sorteo en el canal actual durante un tiempo determinado.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(
                    OptionType.STRING,
                    "premio",
                    "Premio del sorteo",
                    true),
                new OptionData(
                        OptionType.INTEGER,
                        "ganadores",
                        "Cantidad de ganadores",
                        true),
                new OptionData(
                        OptionType.INTEGER,
                        "horas",
                        "Duración del sorteo en horas",
                        true),
                new OptionData(
                        OptionType.STRING,
                        "messageid",
                        "ID de un mensaje existente (opcional) para finalizar un sorteo",
                        false
                ));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        JDA jda = event.getJDA();
        Member member = event.getMember();
        if (member == null) return;

        boolean hasRole = member.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == GIVEAWAY_ROLE_ID);

        if (!hasRole) {
            event.reply("❌ No tienes permiso para gestionar sorteos.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String premio = event.getOption("premio").getAsString();
        int ganadores = event.getOption("ganadores").getAsInt();
        int horas = event.getOption("horas").getAsInt();

        OptionMapping messageIdOption = event.getOption("messageid");

        if (messageIdOption != null) {
            long messageId = messageIdOption.getAsLong();
            TextChannel channel = event.getChannel().asTextChannel();

            channel.retrieveMessageById(messageId).queue(
                    message -> {
                        if (message.getEmbeds().isEmpty()) {
                            event.reply("❌ Este mensaje no contiene un sorteo válido.").setEphemeral(true).queue();
                            return;
                        }

                        var embed = message.getEmbeds().get(0);

                        String description = embed.getDescription();
                        String premioExistente = "";
                        int ganadoresExistentes = 1;

                        if (description != null) {
                            var premioMatcher = java.util.regex.Pattern.compile("🍫 \\*\\*(.+?)\\*\\*").matcher(description);
                            if (premioMatcher.find()) {
                                premioExistente = premioMatcher.group(1);
                            }

                            var ganadoresMatcher = java.util.regex.Pattern.compile("👥 \\*\\*Ganadores:\\*\\* `([0-9]+)`").matcher(description);
                            if (ganadoresMatcher.find()) {
                                ganadoresExistentes = Integer.parseInt(ganadoresMatcher.group(1));
                            }
                        }

                        finishGiveaway(
                                channel.getIdLong(),
                                messageId,
                                ganadoresExistentes,
                                jda,
                                premioExistente
                        );

                        event.reply("✅ Sorteo finalizado usando el mensaje proporcionado.").setEphemeral(true).queue();
                    },
                    error -> {
                        event.reply("❌ No se encontró ningún mensaje con ese ID en este canal, este parámetro es opcional y solo se debe usar cuando se requiere terminar un sorteo de manera manual usando el ID del mensaje").setEphemeral(true).queue();
                    }
            );

            return;
        }

        if (ganadores <= 0 || horas <= 0) {
            event.reply("❌ Valores inválidos.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        String imageUrl = "https://i.imgur.com/lbLR6Pu.png";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎁 ¡SORTEO ACTIVO! 🎁")
                .setDescription(
                        "✨ **¿Qué estamos regalando?**\n" +
                                "🍫 **" + premio + "**\n\n" +

                                "👥 **Ganadores:** `" + ganadores + "`\n" +
                                "⏳ **Duración:** `" + horas + " " + (horas == 1 ? "hora" : "horas") + "`\n\n" +

                                "👉 **¿Cómo participar?**\n" +
                                "Reacciona con ✨ a este mensaje y ¡listo!\n\n" +

                                "🍀 **¡Mucha suerte a todos!**"
                )
                .setImage(imageUrl)
                .setFooter("hobba.tv", FOOTER_ICON)
                .setColor(COLORS.get(RANDOM.nextInt(COLORS.size())));

        channel.sendMessageEmbeds(embed.build()).queue(sentMessage -> {
            sentMessage.addReaction(Emoji.fromUnicode("✨")).queue();

            scheduleFinish(
                    sentMessage.getChannel().getIdLong(),
                    sentMessage.getIdLong(),
                    ganadores,
                    horas,
                    jda,
                    premio
            );
        });

        event.reply("✅ Sorteo creado correctamente.")
                .setEphemeral(true)
                .queue();
    }

    private void scheduleFinish(long channelId, long messageId, int ganadores, int horas, JDA jda, String premio) {
        SCHEDULER.schedule(
                () -> finishGiveaway(channelId, messageId, ganadores, jda, premio),
                horas,
                TimeUnit.HOURS
        );
    }

    private void finishGiveaway(long channelId, long messageId, int ganadores, JDA jda, String premio) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        channel.retrieveMessageById(messageId).queue(message -> {

            message.retrieveReactionUsers(Emoji.fromUnicode("✨")).queue(users -> {

                List<Member> participantes = new ArrayList<>(
                        users.stream()
                                .filter(user -> !user.isBot())
                                .map(user -> channel.getGuild().getMember(user))
                                .filter(Objects::nonNull)
                                .toList()
                );

                if (participantes.isEmpty()) {
                    channel.sendMessage("❌ El sorteo terminó sin participantes válidos.").queue();
                    return;
                }

                Collections.shuffle(participantes);

                List<Member> ganadoresFinales = participantes.stream()
                        .limit(ganadores)
                        .toList();

                String menciones = ganadoresFinales.stream()
                        .map(Member::getAsMention)
                        .collect(Collectors.joining(", "));

                EmbedBuilder resultEmbed = new EmbedBuilder()
                        .setTitle("✨ ¡La suerte eligió a los ganadores de " + premio + "! ✨")
                        .setDescription(
                                "🥳 **Después de mezclar nombres y cruzar dedos...**\n\n" +
                                        "🏆 **Los ganadores son:** " + menciones + "\n\n" +
                                        "🎁 **¿Y el premio?**\n" +
                                        "En un rato lo verás reflejado en tu **inventario** o nos contactaremos contigo 👀✨\n\n" +
                                        "💙 Gracias a todos por participar y atentos al próximo sorteo..."
                        )
                        .setImage("https://i.imgur.com/lbLR6Pu.png")
                        .setFooter("hobba.tv", FOOTER_ICON)
                        .setColor(Color.GREEN);

                channel.sendMessageEmbeds(resultEmbed.build()).queue();
            });
        });
    }
}
