package com.drow.hobbito.events;

import com.drow.hobbito.interfaces.ICommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.drow.hobbito.common.Components.FOOTER_ICON;
import static com.drow.hobbito.events.Giveaway.loadImageFromUrl;

@Slf4j
@RequiredArgsConstructor
public class VouchersByPeriod extends ListenerAdapter implements ICommand {
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final int START_HOUR = 10;
    private static final int END_HOUR = 20;
    private static final long VOUCHERS_CODE_CHANNEL_ID = 1461597989113036841L;
    private static final long VOUCHERS_LOGS_CHANNEL_ID = 1458261437561704604L;
    private static final long USER_ABLE_TO_POST_VOUCHERS_ID = 339140796311797774L;
    private static final long CHANNEL_TO_SEND_VOUCHER_ID = 1415543423334744180L;
    private static final long VOUCHERS_ROLE_ID = 1458251135692574827L;
    private static final String IMAGES_HOST = "https://raw.githubusercontent.com/Drowss/hobba-assets/main";
    private LocalDate lastSentDate = null;
    private Boolean vouchersState = false;
    private Disposable voucherScheduler = null;

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getMember() == null) {
            event.reply("No tienes permisos ❌").setEphemeral(true).queue();
            return;
        }

        boolean hasRole = event.getMember().getRoles().stream()
                .anyMatch(role -> role.getIdLong() == VOUCHERS_ROLE_ID);

        if (!hasRole) {
            event.reply("No tienes permisos ❌").setEphemeral(true).queue();
            return;
        }

        Boolean activar = event.getOption("activar").getAsBoolean();
        String plantilla = event.getOption("plantilla", "voucherBackgroundDefault.png", OptionMapping::getAsString);


        event.reply("Comando ejecutado ✅")
                .setEphemeral(true)
                .queue();

        sendVoucher(event.getJDA(), plantilla);

//        if (activar && !vouchersState) {
//            vouchersState = true;
//            JDA jda = event.getJDA();
//            sendLog(jda, "Programando vouchers diarios...");
//            voucherScheduler = scheduleNext(jda, plantilla)
//                    .subscribe();
//            return;
//        }
//
//        if (!activar && vouchersState) {
//            vouchersState = false;
//
//            if (voucherScheduler != null && !voucherScheduler.isDisposed()) {
//                voucherScheduler.dispose();
//                voucherScheduler = null;
//            }
//
//            sendLog(event.getJDA(), "Apagando vouchers... ⛔");
//        }
    }

    private Mono<Void> scheduleNext(JDA jda, String voucherBackground) {
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);

        if (lastSentDate != null && lastSentDate.isEqual(now.toLocalDate())) {
            ZonedDateTime tomorrow = now
                    .plusDays(1)
                    .withHour(START_HOUR)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            long delayMillis = java.time.Duration.between(now, tomorrow).toMillis();
            return Mono.delay(java.time.Duration.ofMillis(delayMillis))
                    .flatMap(t -> scheduleNext(jda, voucherBackground));
        }

        int randomHour = ThreadLocalRandom.current().nextInt(START_HOUR, END_HOUR);
        int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);
        ZonedDateTime nextRun = now
                .withHour(randomHour)
                .withMinute(randomMinute)
                .withSecond(0)
                .withNano(0);

        if (nextRun.isBefore(now)) {
            nextRun = nextRun.plusDays(1);
        }

        long delayMillis = java.time.Duration.between(now, nextRun).toMillis();

        return Mono.delay(java.time.Duration.ofMillis(delayMillis))
                .flatMap(t -> {
                    sendVoucher(jda, voucherBackground);
                    lastSentDate = ZonedDateTime.now(COLOMBIA).toLocalDate();
                    return scheduleNext(jda, voucherBackground);
                });
    }

    private void sendVoucher(JDA jda, String voucherBackground) {
        TextChannel channel = jda.getTextChannelById(VOUCHERS_CODE_CHANNEL_ID);
        if (channel == null) return;

        channel.getHistory().retrievePast(100).queue(messages -> {
            List<Message> userMessages = messages.stream()
                    .filter(m -> m.getAuthor().getIdLong() == USER_ABLE_TO_POST_VOUCHERS_ID)
                    .toList();

            if (userMessages.isEmpty()) {
                sendLog(jda, "No hay mensajes de este usuario " + userMessages);
                return;
            }

            Message randomMessage = userMessages.get(ThreadLocalRandom.current().nextInt(userMessages.size()));

            try {
                byte[] imageBytes = buildVoucherImage(
                        jda,
                        voucherBackground,
                        randomMessage.getContentRaw()
                );

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("🎟️ ¡VOUCHER DISPONIBLE! 🎟️")
                        .setDescription(
                                "¡Sé el primero en canjearlo!🎉\n\n" +
                                        "💳 **Código:** `" + randomMessage.getContentRaw() + "`"
                        )
                        .setImage("attachment://voucher.png")
                        .setFooter("hobba.tv", FOOTER_ICON)
                        .setColor(Color.ORANGE);

                TextChannel channelToSendEmbed = jda.getTextChannelById(CHANNEL_TO_SEND_VOUCHER_ID);

                channelToSendEmbed.sendMessageEmbeds(embed.build())
                        .addFiles(
                                FileUpload.fromData(imageBytes, "voucher.png")
                        )
                        .queue(
                                success -> sendLog(jda, "Voucher enviado correctamente 🎉 " + randomMessage.getContentRaw()),
                                error -> sendLog(jda, "Error enviando voucher: " + error.getMessage())
                        );
            } catch (Exception e) {
                sendLog(jda, e.getMessage());
            }

            randomMessage.delete().queue(
                    success -> sendLog(jda, "Voucher usado: " + randomMessage.getContentDisplay()),
                    error -> sendLog(jda, "No se pudo eliminar voucher: " + error.getMessage() + " " + error.getClass().getSimpleName())
            );
        });
    }

    private void sendLog(JDA jda, String log) {
        TextChannel channel = jda.getTextChannelById(VOUCHERS_LOGS_CHANNEL_ID);
        if (channel == null) return;

        channel.sendMessage(log).queue();
    }

    private byte[] buildVoucherImage(JDA jda, String voucherBackground, String voucherCode) throws IOException, FontFormatException {
        BufferedImage base = loadImageFromUrl(IMAGES_HOST + "/images/base/" + voucherBackground);
        Graphics2D g = base.createGraphics();
        InputStream fontStream = getClass()
                .getResourceAsStream("/font/poppins.ttf");

        if (fontStream == null) {
            sendLog(jda, "No se encontró la fuente poppins.ttf");
            throw new IllegalStateException("No se encontró la fuente poppins.ttf");
        }

        Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
                .deriveFont(Font.BOLD, 36f);

        g.setFont(font);
        g.setColor(Color.decode("#2e2eff"));

        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(voucherCode, frc);
        FontMetrics metrics = g.getFontMetrics(font);

        int centerX = 285;
        int centerY = 138;

        int x = centerX - (metrics.stringWidth(voucherCode) / 2);
        int y = centerY;

        g.drawString(voucherCode, x, y);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(base, "png", out);

        return out.toByteArray();
    }

    @Override
    public String getName() {
        return "voucher";
    }

    @Override
    public String getDescription() {
        return "Programa vouchers";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(
                        OptionType.BOOLEAN,
                        "activar",
                        "activar/desactivar vouchers",
                        true),
                new OptionData(
                        OptionType.STRING,
                        "plantilla",
                        "plantilla vouchers",
                        false));
    }
}
