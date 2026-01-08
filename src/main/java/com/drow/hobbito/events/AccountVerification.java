package com.drow.hobbito.events;

import com.drow.hobbito.adapter.GetUserAdapter;
import com.drow.hobbito.adapter.response.UserResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.drow.hobbito.common.Components;
import com.drow.hobbito.common.HobbaException;
import com.drow.hobbito.common.HobbaUserCode;
import com.drow.hobbito.interfaces.ICommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static com.drow.hobbito.common.Components.CAFFEINE_TTL;

@Slf4j
@RequiredArgsConstructor
public class AccountVerification extends ListenerAdapter implements ICommand {
    private final Cache<String, HobbaUserCode> userVerificationCodes;
    private final GetUserAdapter getUserAdapter;
    private static final String HOBBA_ICON_URL = "https://i.imgur.com/ePzltxh.png";
    private static final String LOG_CHANNEL_ID = "1432137826685685880";
    private static final String VERIFICATION_ROLE_ID = "790882776970428416";

    @Override
    public String getName() {
        return "verificar";
    }

    @Override
    public String getDescription() {
        return "Verifica tu Hobba usuario con tu usuario de Discord";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(
                OptionType.STRING,
                "usuario",
                "Usuario a verificar",
                true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(interactionHook -> {
            String user = event.getOption("usuario").getAsString();
            String discordUserName = event.getUser().getName();

            sendLog(event, "Iniciando verificación para usuario: " + user + " por Discord user: " + discordUserName);

            if (event.getMember()
                    .getRoles()
                    .stream()
                    .anyMatch(role -> role.getId().equals(VERIFICATION_ROLE_ID))) {

                sendLog(event, "El usuario de Discord " + discordUserName + " ya está verificado como " + user);
                interactionHook.sendMessage("✅ Ya estás verificado como " + discordUserName + " en Hobba.")
                        .queue();
                return;
            } else if (userVerificationCodes.getIfPresent(event.getUser().getId()) != null) {
                sendLog(event, "El usuario de Discord " + discordUserName + " ya tiene un proceso de verificación en curso.");
                interactionHook.sendMessage("❗ Ya tienes un proceso de verificación en curso. Por favor, intenta de " +
                                "nuevo en " + CAFFEINE_TTL + " minutos")
                        .queue();
                return;
            }

            getUserAdapter.getUser(user)
                    .subscribe(userResponse -> {
                                String verificationCode = String.format("%06X", new Random().nextInt(0xFFFFFF));
                                userVerificationCodes.put(event.getUser().getId(), new HobbaUserCode(user, verificationCode));

                                MessageCreateBuilder message = verificationCodeMessage(userResponse, verificationCode);

                                interactionHook.sendMessage(message.build()).queue();
                                sendLog(event, "Código de verificación " + verificationCode + " generado para keko " + user +
                                        " identificado en Discord como " + discordUserName);

                                AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
                                long tries = 10;
                                long intervalMinutes = 10;

                                Disposable subscription = Flux.interval(Duration.ZERO, Duration.ofMinutes(intervalMinutes))
                                        .take(tries)
                                        .flatMap(attempt ->
                                                getUserAdapter.getUser(user)
                                                        .doOnNext(updatedUser -> {
                                                            if (updatedUser.getMotto().equals(verificationCode)) {
                                                                sendLog(event, "✅ Código de verificación coincide para keko " + user +
                                                                        " en intento " + (attempt + 1) + "/" + tries);

                                                                Guild guild = event.getGuild();
                                                                Role verificationRole = guild.getRoleById(VERIFICATION_ROLE_ID);
                                                                guild.addRoleToMember(event.getMember(), verificationRole)
                                                                        .reason("Verificación completada")
                                                                        .queue();

                                                                guild.modifyNickname(event.getMember(), updatedUser.getUsername())
                                                                        .reason("Nombre actualizado tras verificación")
                                                                        .queue();

                                                                EmbedBuilder embed = buildSuccessVerification(event, updatedUser);
                                                                event.getJDA()
                                                                        .getTextChannelById("640666346958487563")
                                                                        .sendMessageEmbeds(embed.build()).queue();

                                                                Disposable s = subscriptionRef.get();
                                                                if (s != null && !s.isDisposed()) s.dispose();
                                                            } else {
                                                                sendLog(event, "Intento " + (attempt + 1) + "/" + tries +
                                                                        " para keko " + updatedUser.getUsername() +
                                                                        " cada " + intervalMinutes + " " +
                                                                        "minutos | " +
                                                                        "Código esperado: " + verificationCode +
                                                                        " | Código actual: " + updatedUser.getMotto());
                                                            }
                                                        })
                                        )
                                        .doOnComplete(() -> {
                                            sendLog(event,
                                                    "No se pudo verificar al usuario " + userResponse.getUsername() + " después de " + tries + " intentos.");
                                            interactionHook.sendMessage(
                                                    "❌ No se pudo completar la verificación. " +
                                                            "Por favor, intenta el proceso nuevamente o dirígete al canal <#1022664522605010954> para verificación manual."
                                            ).queue();
                                        })
                                        .subscribe();

                                subscriptionRef.set(subscription);
                            },
                            error -> {
                                sendLog(event, "Error validando la existencia del usuario: " + error.getMessage());
                                if (error instanceof HobbaException) {
                                    EmbedBuilder embedBuilder = Components.buildFrank(error.getMessage());
                                    interactionHook.sendMessageEmbeds(embedBuilder.build()).queue();
                                }
                            });
        });
    }

    private void sendLog(SlashCommandInteractionEvent event, String message) {
        log.info(message);

        Guild guild = event.getGuild();
        if (guild != null) {
            TextChannel logChannel = guild.getTextChannelById(LOG_CHANNEL_ID);
            if (logChannel != null) {
                logChannel.sendMessage("🪵 " + message).queue();
            } else {
                log.warn("No se encontró el canal de logs con ID: " + LOG_CHANNEL_ID);
            }
        }
    }

    private MessageCreateBuilder verificationCodeMessage(UserResponse userResponse, String verificationCode) {
        return new MessageCreateBuilder()
                .setContent("**🔐 Vamos a verificar a " + userResponse.getUsername() + " \uD83E\uDD13\uD83D\uDC46" +
                        " **")
                .addEmbeds(new EmbedBuilder()
                        .setColor(0x00FF00)
                        .setTitle("📋 Tu Código de Verificación")
                        .setDescription("```" + verificationCode + "```")
                        .addField("¿Cómo usar?",
                                "1. Copia el código de verificación\n" +
                                        "2. Pégalo en la misión de tu perfil en Hobba\n" +
                                        "3. Luego espera, el proceso de verificación puede tardar hasta una hora ⏳...",
                                false)
                        .setImage(HOBBA_ICON_URL)
                        .setFooter("hobba.tv",
                                "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png?ex=68c4cc9f&is=68c37b1f&hm=e11645338c8b0e57ea47132114bf3917206b855f0fbb347a125a0e3d89657a37&=&format=webp&quality=lossless")
                        .build());
    }

    private EmbedBuilder buildSuccessVerification(SlashCommandInteractionEvent event, UserResponse userResponse) {
        return new EmbedBuilder()
                .setColor(0x00FF00)
                .setTitle("🎉 ¡Verificación completada!")
                .setDescription("Bienvenido/a, " + event.getUser().getAsMention() +
                        ". Tu keko a nombre de **" + userResponse.getUsername() + "** ha sido confirmado.\n" +
                        "Las puertas del **Hotel Hobba** están abiertas para ti. 🛎️✨\n")
                .setImage("https://www.hobba" +
                        ".tv/habblet/avatarimageByUsername/" + userResponse.getUsername() +
                        "?size=undefined&direction=0&head_direction=2&headonly=0&action=std&gesture=std")
                .setFooter("hobba.tv",
                        "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png?ex=68c4cc9f&is=68c37b1f&hm=e11645338c8b0e57ea47132114bf3917206b855f0fbb347a125a0e3d89657a37&=&format=webp&quality=lossless");

    }
}
