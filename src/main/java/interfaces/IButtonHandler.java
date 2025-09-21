package interfaces;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface IButtonHandler {
    String getButtonId();
    void handleButtonClick(ButtonInteractionEvent event);
}
