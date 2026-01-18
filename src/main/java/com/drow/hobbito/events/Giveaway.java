package com.drow.hobbito.events;

import com.drow.hobbito.interfaces.ICommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.drow.hobbito.common.Components.FOOTER_ICON;
import static com.drow.hobbito.common.Components.buildAvatarUrl;

@Slf4j
@RequiredArgsConstructor
public class Giveaway extends ListenerAdapter implements ICommand {
    private static final long GIVEAWAY_ROLE_ID = 1460304976428531936L;
    private static final long GIVEAWAY_ERROR_CHANNEL_ID = 1462283179284107458L;
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();
    private static final ExecutorService IMAGE_EXECUTOR =
            Executors.newFixedThreadPool(2);
    private static final List<String> BASE_IMAGE_URLS = List.of(
            "https://i.imgur.com/YQEEylM.png",
            "https://i.imgur.com/daBL3Xu.png",
            "https://i.imgur.com/9GVahGr.png",
            "https://i.imgur.com/j2iYcAT.png"
    );
    private static final String BASE_WINNER_IMAGE_URL = "https://i.imgur.com/RiRJAGn.png";

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
                        "imagenid",
                        "ID de imagen del premio (opcional)",
                        false),
                new OptionData(
                        OptionType.STRING,
                        "messageid",
                        "ID de un mensaje existente (opcional) para finalizar un sorteo",
                        false
                ));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            event.deferReply(true).queue();
            JDA jda = event.getJDA();
            Member member = event.getMember();
            if (member == null) return;

            boolean hasRole = member.getRoles().stream()
                    .anyMatch(role -> role.getIdLong() == GIVEAWAY_ROLE_ID);

            if (!hasRole) {
                event.getHook().sendMessage("❌ No tienes permiso para gestionar sorteos.")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String premio = event.getOption("premio").getAsString();
            int ganadores = event.getOption("ganadores").getAsInt();
            int horas = event.getOption("horas").getAsInt();

            OptionMapping messageIdOption = event.getOption("messageid");

            if (messageIdOption != null) {
                long messageId;
                try {
                    messageId = messageIdOption.getAsLong();
                } catch (NumberFormatException e) {
                    event.getHook().sendMessage("❌ No se encontró ningún mensaje con ese ID en este canal, este parámetro es opcional y solo se debe usar cuando se requiere terminar un sorteo de manera manual usando el ID del mensaje").queue();
                    return;
                }

                TextChannel channel = event.getChannel().asTextChannel();

                channel.retrieveMessageById(messageId).queue(
                        message -> {
                            if (message.getEmbeds().isEmpty()) {
                                event.getHook().sendMessage("❌ Este mensaje no contiene un sorteo válido.").setEphemeral(true).queue();
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

                            if (premioExistente.isBlank()) {
                                event.getHook().sendMessage("❌ No se pudo extraer el premio del sorteo existente.").setEphemeral(true).queue();
                                return;
                            }
                            finishGiveaway(
                                    channel.getIdLong(),
                                    messageId,
                                    ganadoresExistentes,
                                    jda,
                                    premioExistente
                            );

                            event.getHook().sendMessage("✅ Sorteo finalizado usando el mensaje proporcionado.").queue();
                        },
                        error -> {
                            event.getHook().sendMessage("❌ No se encontró ningún mensaje con ese ID en este canal, este parámetro es opcional y solo se debe usar cuando se requiere terminar un sorteo de manera manual usando el ID del mensaje").queue();
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

            IMAGE_EXECUTOR.submit(() -> {
                try {
                    String imageHost = "https://i.imgur.com/";
                    OptionMapping imageOption = event.getOption("imagenid");

                    String overlayImageId = "mq2NOLF.png";

                    if (imageOption != null) {
                        String imagenPremio = imageOption.getAsString();
                        if (!imagenPremio.isBlank()) {
                            overlayImageId = imagenPremio;
                        }
                    }
                    BufferedImage base = loadImageFromUrl(BASE_IMAGE_URLS.get(RANDOM.nextInt(BASE_IMAGE_URLS.size())));
                    BufferedImage overlay = loadImageFromUrl(imageHost + overlayImageId);

                    Graphics2D g = base.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int centerX = base.getWidth() / 2;
                    int centerY = base.getHeight() / 2;

                    int drawX = centerX - overlay.getWidth() / 2;
                    int drawY = centerY - overlay.getHeight() / 2;

                    g.drawImage(overlay, drawX, drawY, null);
                    g.dispose();

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(base, "png", os);
                    byte[] imageBytes = os.toByteArray();

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
                            .setImage("attachment://sorteo.png")
                            .setFooter("hobba.tv", FOOTER_ICON)
                            .setColor(Color.GREEN);

                    channel.sendFiles(FileUpload.fromData(imageBytes, "sorteo.png"))
                            .setEmbeds(embed.build())
                            .queue(sentMessage -> {
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

                    event.getHook().sendMessage("✅ Sorteo creado correctamente.").queue();

                } catch (Exception e) {
                    sendTrace(event.getJDA(), e);
                }
            });
        } catch (Exception e) {
            sendTrace(event.getJDA(), e);
        }

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
                List<User> participantes = new ArrayList<>(
                        users.stream()
                                .filter(user -> !user.isBot())
                                .toList()
                );

                log.info("participantes reales: {}", participantes);

                if (participantes.isEmpty()) {
                    channel.sendMessage("❌ El sorteo terminó sin participantes válidos.").queue();
                    return;
                }

                Collections.shuffle(participantes);

                List<User> ganadoresFinales = participantes.stream()
                        .limit(ganadores)
                        .toList();

                log.info("ganadores finales: {}", ganadoresFinales);
                Guild guild = channel.getGuild();

                List<String> displayNames = new ArrayList<>();

                AtomicInteger pending = new AtomicInteger(ganadoresFinales.size());

                String menciones = ganadoresFinales.stream()
                        .map(User::getAsMention)
                        .collect(Collectors.joining(", "));

                for (User user : ganadoresFinales) {
                    guild.retrieveMemberById(user.getId()).queue(member -> {

                        String displayName = member.getNickname() != null
                                ? member.getNickname()
                                : member.getUser().getName();

                        displayNames.add(displayName);

                        if (pending.decrementAndGet() == 0) {
                            renderImageAndSend(channel, displayNames, menciones, premio, jda);
                        }

                    }, error -> {
                        displayNames.add("drow");

                        if (pending.decrementAndGet() == 0) {
                            renderImageAndSend(channel, displayNames, menciones, premio, jda);
                        }
                    });
                }
            });
        });
    }

    public static BufferedImage loadImageFromUrl(String url) throws IOException {
        URLConnection con = new URL(url).openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        );

        try (InputStream in = con.getInputStream()) {
            return ImageIO.read(in);
        }
    }

    private void renderImageAndSend(
            TextChannel channel,
            List<String> displayNames,
            String menciones,
            String premio,
            JDA jda
    ) {
        try {
            BufferedImage base = loadImageFromUrl(BASE_WINNER_IMAGE_URL);
            Graphics2D g = base.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int winners = Math.min(displayNames.size(), 5);
            int avatarWidth = 64;
            int avatarHeight = 110;
            int spacing = 30;

            int centerX = base.getWidth() / 2;
            int centerY = base.getHeight() / 2;

            int totalBlockWidth =
                    winners * avatarWidth + (winners - 1) * spacing;

            int startX = centerX - totalBlockWidth / 2;
            int drawY = centerY - avatarHeight / 2;

            for (int i = 0; i < winners; i++) {
                String name = displayNames.get(i);
                String urlAvatar = buildAvatarUrl(name);

                BufferedImage avatar = loadImageFromUrl(urlAvatar);

                int drawX = startX + i * (avatarWidth + spacing);

                g.drawImage(
                        avatar,
                        drawX,
                        drawY,
                        avatarWidth,
                        avatarHeight,
                        null
                );
            }

            g.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(base, "png", os);

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✨ ¡La suerte eligió a los ganadores de " + premio + "! ✨")
                    .setDescription(
                            "🥳 **Después de mezclar nombres y cruzar dedos...**\n\n" +
                                    "🏆 **Los ganadores son:** " + menciones + "\n\n" +
                                    "🎁 **¿Y el premio?**\n" +
                                    "En un rato lo verás reflejado en tu **inventario** o nos estaremos " +
                                    "contactando contigo 👀✨\n\n" +
                                    "💙 Gracias a todos por participar"
                    )
                    .setImage("attachment://ganadores.png")
                    .setColor(Color.YELLOW);

            channel.sendMessageEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(os.toByteArray(), "ganadores.png"))
                    .queue();

        } catch (Exception e) {
            sendTrace(jda, e);
        }
    }

    private void sendTrace(JDA jda, Throwable e) {
        TextChannel channel = jda.getTextChannelById(GIVEAWAY_ERROR_CHANNEL_ID);
        if (channel == null) return;

        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));

        String trace = sw.toString();
        if (trace.length() > 1900) {
            trace = trace.substring(0, 1900);
        }

        channel.sendMessage(trace).queue();
    }
}
