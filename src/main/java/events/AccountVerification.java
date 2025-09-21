package events;

import interfaces.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.List;
import java.util.Random;

public class AccountVerification implements ICommand {
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
        String verificationCode = String.format("%06X", new Random().nextInt(0xFFFFFF));
        String imageUrl = "https://i.imgur.com/ePzltxh.png";

        MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("**🔐 Vamos a verificar a " + event.getOption("usuario").getAsString() + " \uD83E\uDD13\uD83D\uDC46 **")
                .addEmbeds(new EmbedBuilder()
                        .setColor(0x00FF00)
                        .setTitle("📋 Tu Código de Verificación")
                        .setDescription("```" + verificationCode + "```")
                        .addField("¿Cómo usar?", "1. Copia el código de verificación\n2. Pégalo en la misión de tu perfil en Hobba\n3. Click en el botón ya verifiqué", false)
                        .setImage(imageUrl)
                        .setFooter("https://hobba.tv",
                                "https://media.discordapp.net/attachments/1415545619757531189/1415875828989820968/image.png?ex=68c4cc9f&is=68c37b1f&hm=e11645338c8b0e57ea47132114bf3917206b855f0fbb347a125a0e3d89657a37&=&format=webp&quality=lossless")
                        .build());

        event.reply(message.build())
                .addActionRow(
                        Button.success("success_verification", "✅ Ya verifiqué")
                )
                .setEphemeral(true)
                .queue();
    }
}
