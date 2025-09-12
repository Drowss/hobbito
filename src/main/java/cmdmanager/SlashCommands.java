package cmdmanager;

import interfaces.ICommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

@Slf4j
public class SlashCommands extends ListenerAdapter {
    private List<ICommand> commands;

    public SlashCommands(List<ICommand> commands) {
        this.commands = commands;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        for (ICommand cmd : commands) {
            if (cmd.getName().equals(event.getName())) {
                cmd.execute(event);
                return;
            }
        }
    }
}
