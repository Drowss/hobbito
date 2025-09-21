package events;

import interfaces.IButtonHandler;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ButtonSuccessVerification implements IButtonHandler {
    @Override
    public String getButtonId() {
        return "success_verification";
    }

    @Override
    public void handleButtonClick(ButtonInteractionEvent event) {
        Role verificationRole = event.getGuild().getRolesByName("Verificado", true).get(0);

        event.getGuild().addRoleToMember(event.getMember(), verificationRole)
                .reason("Verificación completada mediante botón")
                .queue(
                        success -> {
                            event.reply("✅ ¡Felicidades " + event.getUser().getAsMention() + "! Se te ha asignado el rol " + verificationRole.getAsMention() + ".")
                                    .setEphemeral(false)
                                    .queue();
                        },
                        error -> {
                            event.reply("❌ No pude asignarte el rol. Error: " + error.getMessage())
                                    .setEphemeral(true)
                                    .queue();
                        }
                );
    }
}
