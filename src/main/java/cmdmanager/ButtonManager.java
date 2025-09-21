package cmdmanager;

import interfaces.IButtonHandler;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ButtonManager extends ListenerAdapter {
    private final Map<String, IButtonHandler> buttonHandlers = new HashMap<>();

    public ButtonManager(List<IButtonHandler> handler) {
        handler.forEach(button -> buttonHandlers.put(button.getButtonId(), button));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        IButtonHandler handler = buttonHandlers.get(buttonId);

        handler.handleButtonClick(event);
    }
}
