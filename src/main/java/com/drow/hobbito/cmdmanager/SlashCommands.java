package com.drow.hobbito.cmdmanager;

import com.drow.hobbito.interfaces.ICommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

@Slf4j
public class SlashCommands extends ListenerAdapter {
    private final List<ICommand> commands;
    private static final String VERIFICATION_CHANNEL_ID = "1457059931848183931";
    private static final String ISSUA_USER_ID = "658161685361590274";
    private static final String DROW_USER_ID = "339140796311797774";

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

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String authorId = event.getAuthor().getId();
        if (event.getAuthor().isBot() || authorId.equals(ISSUA_USER_ID) || authorId.equals(DROW_USER_ID)) return;
        if (!event.getChannel().getId().equals(VERIFICATION_CHANNEL_ID)) return;

        event.getMessage().delete().queue();
    }
}
