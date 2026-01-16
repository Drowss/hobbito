package com.drow.hobbito.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public class Tickets extends ListenerAdapter {
    private final List<String> allowedGuilds;
    private static final long HELPER_ROLE_ID = 791721579658608670L;
    private static final long TICKETS_CATEGORY_ID = 1381795369842905179L;
    private static final long TICKET_LOGS_CHANNEL_ID = 1458331432123498669L;
    private static final long TEXT_CHANNEL_TICKET_ID = 1458330941297791097L;
    private static final long COMMUNITY_GLOBAL_ROLE_ID = 790882776970428416L;

    //pa probar en el sv de pruebas jaj
//    private static final long HELPER_ROLE_ID = 1458251135692574827L;
//    private static final long TICKETS_CATEGORY_ID = 1458238145526239404L;
//    private static final long TICKET_LOGS_CHANNEL_ID = 1458261437561704604L;
//    private static final long TEXT_CHANNEL_TICKET_ID = 1458238171845365925L;
//    private static final long COMMUNITY_GLOBAL_ROLE_ID = 1419253526189838416L;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        String content = message.getContentRaw().trim();

        if (!content.equalsIgnoreCase("!close")) return;

        TextChannel channel = event.getChannel().asTextChannel();

        if (!channel.getName().startsWith("\uD83D\uDCE8│ticket-")) return;

        Member member = event.getMember();
        if (member == null) return;

        boolean hasRole = member.getRoles().stream()
                .anyMatch(role -> role.getIdLong() == HELPER_ROLE_ID);

        if (!hasRole) return;

        message.delete().queue();

        channel.sendMessage("""
        🔔 **¿Ya resolviste tu duda?**
        
        Si ya no tienes más preguntas o tu problema fue solucionado,
        ayúdanos cerrando este ticket presionando abajo para que podamos seguir ayudando a otros usuarios.
        """)
                .setActionRow(
                        Button.danger("ticket:close", "🔒 Cerrar ticket")
                )
                .queue();
    }

    @Override
    public void onReady(ReadyEvent event) {
        Guild guild = event.getJDA().getGuildById(allowedGuilds.get(1));
        TextChannel channel = guild.getTextChannelById(TEXT_CHANNEL_TICKET_ID);

        String ticketMessageContent =
                "🧑‍✈️ **¿Necesitas ayuda?**\n\n" +
                        "Si tienes un problema, duda o necesitas soporte dentro del hotel,\n" +
                        "haz clic en el botón de abajo para abrir un ticket.\n\n" +
                        "🎫 *Te atenderemos lo antes posible.*";

        channel.getHistory().retrievePast(10).queue(messages -> {
            boolean exists = messages.stream()
                    .anyMatch(msg ->
                            msg.getAuthor().isBot()
                                    && msg.getContentRaw().equals(ticketMessageContent)
                    );

            if (exists) {
                return;
            }

            channel.sendMessage(ticketMessageContent)
                    .setActionRow(
                            Button.primary("ticket:create", "🎫 Abrir ticket")
                    )
                    .queue(message -> {
                        long messageId = message.getIdLong();
                        log.info("Mensaje de tickets creado con ID {}", messageId);
                    });
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("ticket:create")) {
            Guild guild = event.getGuild();
            Member member = event.getMember();
            String channelName = "\uD83D\uDCE8│ticket-" + member.getEffectiveName().toLowerCase();

            if (!guild.getTextChannelsByName(channelName, true).isEmpty()) {
                TextChannel ticketChannel = guild.getTextChannelsByName(channelName, true).get(0);
                event.reply(
                                "⚠️ Ya tienes un ticket abierto.\n" +
                                        "Continúa la conversación en " + ticketChannel.getAsMention()
                        )
                        .setEphemeral(true)
                        .queue();
                return;
            }
            this.createTicketChannel(guild, member, channelName, event);
        }

        if (id.equals("ticket:close")) {
            closeTicketButton(event);
            return;
        }

        if (id.equals("ticket:close:confirm")) {
            confirmCloseTicket(event);
            return;
        }

        if (id.equals("ticket:close:cancel")) {
            rejectCloseTicket(event);
        }
    }

    private void createTicketChannel(Guild guild, Member member, String channelName, ButtonInteractionEvent event) {
        Role staffRole = guild.getRoleById(HELPER_ROLE_ID);
        Category category = guild.getCategoryById(TICKETS_CATEGORY_ID);

        category.createTextChannel(channelName)
                .addPermissionOverride(
                        guild.getPublicRole(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .addPermissionOverride(
                        member,
                        EnumSet.of(
                                Permission.VIEW_CHANNEL,
                                Permission.MESSAGE_SEND,
                                Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_ATTACH_FILES,
                                Permission.MESSAGE_EMBED_LINKS
                        ),
                        null
                )
                .addPermissionOverride(
                        staffRole,
                        EnumSet.of(
                                Permission.VIEW_CHANNEL,
                                Permission.MESSAGE_SEND,
                                Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_ATTACH_FILES,
                                Permission.MESSAGE_EMBED_LINKS
                        ),
                        null
                )
                .queue(channel -> {
                    channel.sendMessage(
                            "🎫 **Ticket de " + member.getAsMention() + "**\n" +
                                    "Un Hobba te atenderá pronto. \n" + staffRole.getAsMention()
                    )
                    .setActionRow(Button.danger("ticket:close", "🔒 Cerrar ticket"))
                    .queue();
                    event.reply(
                                    "\uD83E\uDDD1\u200D\uD83D\uDCBC\uD83C\uDFAB Dirigete a " + channel.getAsMention() + " para conversar sobre tus inquietudes."
                            )
                            .setEphemeral(true)
                            .queue();
                    Role unwantedRole = guild.getRoleById(COMMUNITY_GLOBAL_ROLE_ID);
                    if (unwantedRole != null) {
                        PermissionOverride override =
                                channel.getPermissionOverride(unwantedRole);

                        if (override != null) {
                            override.delete().queue();
                        }
                    }
                });
    }

    private void closeTicketButton(ButtonInteractionEvent event) {
        event.reply(
                        "⚠️ **¿Estás seguro de cerrar el ticket?**\n\n" +
                                "Al cerrarlo, **el canal será eliminado** y **se perderá todo el historial del chat**.\n" +
                                "Esta acción no se puede deshacer."
                )
                .setActionRow(
                        Button.primary("ticket:close:confirm", "✅ Confirmar"),
                        Button.secondary("ticket:close:cancel", "❌ Cancelar")
                )
                .setEphemeral(true)
                .queue();
    }

    private void confirmCloseTicket(ButtonInteractionEvent event) {
        event.deferEdit().queue(hook -> {
            hook.editOriginalComponents(
                    ActionRow.of(
                            Button.primary("ticket:close:confirm", "✅ Confirmar").asDisabled(),
                            Button.secondary("ticket:close:cancel", "❌ Cancelar").asDisabled()
                    )
            ).queue();
        });

        TextChannel ticketChannel = event.getChannel().asTextChannel();
        Guild guild = event.getGuild();
        TextChannel logsChannel = guild.getTextChannelById(TICKET_LOGS_CHANNEL_ID);

        List<Message> messages = new ArrayList<>();
        List<Message.Attachment> allAttachments = new ArrayList<>();

        // 1️⃣ SOLO recolectamos mensajes
        ticketChannel.getIterableHistory()
                .cache(false)
                .forEachAsync(message -> {
                    messages.add(message);
                    return true;
                })
                .thenRun(() -> {

                    messages.sort(
                            (a, b) -> a.getTimeCreated().compareTo(b.getTimeCreated())
                    );

                    for (Message msg : messages) {
                        if (!msg.getAttachments().isEmpty()) {
                            allAttachments.addAll(msg.getAttachments());
                        }
                    }

                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                                    .withLocale(new Locale("es", "CO"));

                    StringBuilder chatLog = new StringBuilder();
                    chatLog.append("🧾 Chatlog del **")
                            .append(ticketChannel.getName())
                            .append("** cerrado por ")
                            .append(event.getMember().getAsMention())
                            .append("\n\n");

                    int attachmentCounter = 1;
                    for (Message msg : messages) {

                        ZonedDateTime colombiaTime =
                                msg.getTimeCreated()
                                        .atZoneSameInstant(ZoneId.of("America/Bogota"));

                        Member member = msg.getMember();

                        String displayName = (member != null)
                                ? member.getEffectiveName()
                                : msg.getAuthor().getName();

                        String content = msg.getContentDisplay();

                        chatLog.append("[")
                                .append(colombiaTime.format(formatter))
                                .append("] ")
                                .append(displayName)
                                .append(": ");

                        if (!content.isBlank()) {
                            chatLog.append(content);
                        }

                        if (!msg.getAttachments().isEmpty()) {
                            for (int i = 0; i < msg.getAttachments().size(); i++) {
                                chatLog.append("\n   📎 Adjunto ")
                                        .append(attachmentCounter++);
                            }
                        }

                        chatLog.append("\n");
                    }

                    int maxLength = 1900;
                    for (int i = 0; i < chatLog.length(); i += maxLength) {
                        int end = Math.min(chatLog.length(), i + maxLength);
                        logsChannel.sendMessage(chatLog.substring(i, end)).queue();
                    }

                    uploadAttachmentsInBatches(allAttachments, logsChannel)
                            .thenRun(() -> ticketChannel.delete().queue());
                });
    }

    private void rejectCloseTicket(ButtonInteractionEvent event) {
        event.deferEdit().queue();
        event.getHook().deleteOriginal().queue();
    }

    private CompletableFuture<Void> uploadAttachmentsInBatches(
            List<Message.Attachment> attachments,
            TextChannel logsChannel
    ) {
        final int BATCH_SIZE = 10;
        List<CompletableFuture<Void>> allFutures = new ArrayList<>();

        for (int i = 0; i < attachments.size(); i += BATCH_SIZE) {
            int end = Math.min(attachments.size(), i + BATCH_SIZE);
            List<Message.Attachment> batch = attachments.subList(i, end);

            CompletableFuture<Void> batchFuture =
                    CompletableFuture
                            .allOf(
                                    batch.stream()
                                            .map(Message.Attachment::retrieveInputStream)
                                            .toArray(CompletableFuture[]::new)
                            )
                            .thenAccept(v -> {
                                List<FileUpload> uploads = new ArrayList<>();

                                for (int j = 0; j < batch.size(); j++) {
                                    InputStream stream = batch.get(j)
                                            .retrieveInputStream()
                                            .join();

                                    String uniqueName =
                                            System.currentTimeMillis()
                                                    + "_" + j + "_"
                                                    + batch.get(j).getFileName();

                                    uploads.add(
                                            FileUpload.fromData(stream, uniqueName)
                                    );
                                }

                                logsChannel.sendFiles(uploads).complete();
                            });

            allFutures.add(batchFuture);
        }

        return CompletableFuture.allOf(
                allFutures.toArray(new CompletableFuture[0])
        );
    }
}
